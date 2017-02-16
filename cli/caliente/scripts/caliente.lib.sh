#!/bin/bash
BASEDIR="$(dirname "$(readlink -f "${0}")")"

#
# Initialize the log, if any
#
init_log() {
	# If no log is defined, don't start one
	[ -z "${LOG}" ] && LOG="/dev/null"
	[ -f /dev/fd/9 ] || exec 9>"${LOG}" || {
		echo "FAILED TO INITIALIZE THE LOG AT [${LOG}]"
		exit 9
	}
}

#
# Output the current date in ISO8601 format
#
datemark() {
	date -Iseconds
}

#
# Simply output a message, appending a copy to the log
#
say() {
	init_log
	echo -e "$(datemark): ${@}" | tee -a /dev/fd/9
}

#
# Copy stdin to the log without mirroring it on output
#
silent_log() {
	init_log
	tee -a /dev/fd/9 &>/dev/null
}

#
# Output a message into the log without mirroring it on output
#
log() {
	say "${@}" &>/dev/null
}

#
# Output a message on STDERR without recording it to the log
#
err() {
	echo -e "${@}" 1>&2
}

#
# Fail the script abruptly with exit code ${FAILCODE}, or 2 if not defined
#
fail() {
	err "ERROR: ${@}"
	exit ${FAILCODE:-2}
}

#
# Output a brief usage message and exit with status 2
#
usage() {
	err "usage: ${SCRIPT}${USAGE:+ }${USAGE}"
	for e in "${@}" ; do
		err "\t${e}"
	done
	exit 2
}

#
# Parse out a raw JSON value using JQ (i.e. no quotes)
#
parse_value() {
	parse --raw-output "${@}"
}

#
# Parse out a JSON structure using JQ
#
parse() {
	"${JQ}" "${@}"
}

#
# URLEncode a value
#
urlencode() {
	local OLD_LC_COLLATE="${LC_COLLATE}"
	LC_COLLATE="C"

	local LEN="${#1}"
	for (( i = 0; i < LEN; i++ )); do
		local c="${1:i:1}"
		case "${c}" in
			[a-zA-Z0-9.~_-] ) printf "$c" ;;
			" " ) printf "+" ;;
			* ) printf '%%%02X' "'$c" ;;
		esac
	done

	LC_COLLATE="${OLD_LC_COLLATE}"
}

#
# URLDecode a value
#
urldecode() {
	local STR="${1//+/ }"
	printf '%b' "${STR//%/\\x}"
}

#
# Perform a web service call using CURL
#
curl_call() {
	local URL="${1}"
	local METHOD="${2:-GET}"
	"${CURL}" -k -f -X "${METHOD}" -u "${ALF_USER}:${ALF_PASS}" --url "${URL}" --retry 10 --retry-delay 10
}

#
# Perform a web service call using WGET
#
wget_call() {
	local URL="${1}"
	local METHOD="${2:-GET}"
	case "${METHOD}" in
		GET ) "${WGET}" --tries 10 --waitretry 10 --user="${ALF_USER}" --password="${ALF_PASS}" -O - "${URL}" ;;
		POST )
			# Split out the parameters, turn it into post data
			local PARAMS="${URL#*\?}"
			URL="${URL%%\?*}"

			local P=()
			IFS="&" read -r -a P <<< "${PARAMS}"
			PARAMS=""
			for p in "${P[@]}" ; do
				case "${p}" in
					*=*	) ;;
					*	) PARAMS+="${PARAMS:+&}${p}" ; continue ;;
				esac

				# Name is everything until the first equals symbol
				local name="${p%%=*}"
				# Value is everything after that first equals symbol
				local value="${p#*=}"
				name="$(urlencode "${name}")"
				value="$(urlencode "${value}")"
				PARAMS+="${PARAMS:+&}${name}=${value}"
			done

			"${WGET}" --tries 10 --waitretry 10 --user="${ALF_USER}" --password="${ALF_PASS}" -O --post-data="${PARAMS}" - "${URL}"
			;;
		*	) err "The WGET module only supports GET and POST at this time" ; return 1 ;;
	esac
}

#
# Perform an Alfresco web service call using either WGET or CURL, as configured
# via the CALL variable.  If not defined, it will use WGET
#
call_alfresco() {
	local OUT=""
	local RC=0
	{
		OUT="$("${CALL:-wget_call}" "${@}" 2>&1 1>&3-)"
		RC=${?}
	} 3>&1
	[ ${RC} -ne 0 ] && err "${OUT}"
	return ${RC}
}

#
# Locate an executable in the path, failing the script if it's not found and the executable
# is marked as required
#
find_exe() {
	local EXE_NAME="${1}"
	local REQUIRED="${2}"
	REQUIRED="${REQUIRED,,}"
	case "${REQUIRED}" in
		true | false ) ;;
		* ) REQUIRED="false" ;;
	esac

	local EXE="$(which "${EXE_NAME}")"
	[ -z "${EXE}" ] && EXE="/usr/bin/${EXE_NAME}"
	[ -f "${EXE}" ] || { ${REQUIRED} && fail "'${EXE_NAME}' is not installed, but is required by this script" ; return 1 ; }
	[ -x "${EXE}" ] || { ${REQUIRED} && fail "'${EXE}' is not executable, but is required by this script" ; return 2 ; }
	echo "${EXE}"
	return 0
}

#
# Locate a script that's part of this script package
#
find_script() {
	local EXE_NAME="${1}"
	local REQUIRED="${2}"
	REQUIRED="${REQUIRED,,}"
	case "${REQUIRED}" in
		true | false ) ;;
		* ) REQUIRED="false" ;;
	esac

	local EXE="${BASEDIR}/${EXE_NAME}"
	[ -f "${EXE}" ] || { ${REQUIRED} && fail "'${EXE_NAME}' is not installed, but is required by this script" ; return 1 ; }
	[ -x "${EXE}" ] || { ${REQUIRED} && fail "'${EXE}' is not executable, but is required by this script" ; return 2 ; }
	echo "${EXE}"
	return 0
}

#
# Parse out the authentication data from the authentication file
#
parse_auth() {
	local SRC="${1}"
	local DATA="$(cat "${ALF_AUTH}")"
	parse "." <<< "${DATA}" &>/dev/null || fail "The authentication file [${SRC}] must have proper JSON syntax"
	ALF_USER="$(parse_value ".user" <<< "${DATA}")"
	ALF_PASS="$(parse_value ".password" <<< "${DATA}")"
}

#
# Normalize a path to ensure it contains no relative (. or ..) or empty elements
#
normalize_path() {
	# Remove all . and .. sequences
	# Returns 0 if successful, 1 if the .. recursion goes too far up
	local SRC=()
	local TGT=()
	local IDX=()
	IFS="/" read -r -a SRC <<< "${1}"
	local LEADING=false
	local FIRST=true
	for e in "${SRC[@]}" ; do
		case "${e}" in
			.. )
				[ ${#TGT[@]} -lt 1 ] && return 1
				IDX=(${!TGT[@]});
				unset TGT[${IDX[@]: -1}]
				;;
			"" ) ${FIRST} && LEADING=true ;;
			. ) ;; # Do nothing - must be silently skipped
			* ) TGT+=("${e}") ;;
		esac
		FIRST=false
	done
	${LEADING} || FIRST=true
	for e in "${TGT[@]}" ; do
		${FIRST} || echo -en "/"
		echo -en "${e}"
		FIRST=false
	done
	${FIRST} && echo -en "/"
	echo ""
	return 0
}
