#!/bin/bash
#*******************************************************************************
# #%L
# Armedia Caliente
# %%
# Copyright (C) 2013 - 2019 Armedia, LLC
# %%
# This file is part of the Caliente software.
#
# If the software was purchased under a paid Caliente license, the terms of
# the paid license agreement will prevail.  Otherwise, the software is
# provided under the following open source license terms:
#
# Caliente is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Caliente is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with Caliente. If not, see <http://www.gnu.org/licenses/>.
# #L%
#*******************************************************************************
LIB="$(readlink -f "${0}")"
LIBDIR="$(dirname "${LIB}")"
LIB="$(basename "${LIB}")"

__LOG_ALTERNATES=()
__LOG_ALTERNATES+=("/var/tmp/caliente")
__LOG_ALTERNATES+=("/tmp/caliente")
__LOG_ALTERNATES+=("${LIBDIR}")
__LOG_ALTERNATES+=("${PWD}")

#
# Initialize the log, if any
#
__do_init_log() {
	local L="${1}"
	local D="$(dirname "${L}")"
	[ -f /dev/fd/9 ] && return 0
	[ -d "${D}" ] || mkdir -p "${D}" &>/dev/null || return 1
	exec 9>"${L}" || return 1
	return 0
}

init_log() {
	if [ -z "${LOG}" ] ; then
		LOG="/dev/null"
		__do_init_log "${LOG}" && return 0
	else
		local LOGDIR=()
		local LOGNAME="$(basename "${LOG}")"
		LOGDIR+=("$(dirname "${LOG}")")
		LOGDIR+=("${__LOG_ALTERNATES[@]}")
		for LD in "${LOGDIR[@]}" ; do
			NEW_LOG="${LD}/${LOGNAME}"
			if __do_init_log "${NEW_LOG}" ; then
				LOG="${NEW_LOG}"
				return 0
			fi
		done
	fi
	echo "FAILED TO INITIALIZE THE LOG [${LOG}]"
	exit 9
}

#
#
# Initialize the candidate log, if any
#
__do_init_candidate_log() {
	local L="${1}"
	local D="$(dirname "${L}")"
	[ -f /dev/fd/8 ] && return 0
	[ -d "${D}" ] || mkdir -p "${D}" &>/dev/null || return 1
	exec 8>"${L}" || return 1
	return 0
}

init_candidate_log() {
	if [ -z "${CANDIDATE_LOG}" ] ; then
		CANDIDATE_LOG="/dev/null"
		__do_init_candidate_log "${CANDIDATE_LOG}" && return 0
	else
		local LOGDIR=()
		local LOGNAME="$(basename "${CANDIDATE_LOG}")"
		LOGDIR+=("$(dirname "${CANDIDATE_LOG}")")
		LOGDIR+=("${__LOG_ALTERNATES[@]}")
		for LD in "${LOGDIR[@]}" ; do
			NEW_CANDIDATE_LOG="${LD}/${LOGNAME}"
			if __do_init_candidate_log "${NEW_CANDIDATE_LOG}" ; then
				CANDIDATE_LOG="${NEW_CANDIDATE_LOG}"
				return 0
			fi
		done
	fi
	echo "FAILED TO INITIALIZE THE CANDIDATE LOG AT [${CANDIDATE_LOG}]"
	exit 9
}

#
# Initialize the index map
#
__do_init_index_map() {
	local L="${1}"
	local D="$(dirname "${L}")"
	[ -f "${L}" ] && return 0
	[ -d "${D}" ] || mkdir -p "${D}" &>/dev/null || return 1
	touch "${L}" &>/dev/null
	return ${?}
}

init_index_map() {
	if [ -z "${INDEX_MAP}" ] ; then
		INDEX_MAP="/dev/null"
		__do_init_index_map "${INDEX_MAP}" && return 0
	else
		local MAPDIR=()
		local MAPNAME="$(basename "${INDEX_MAP}")"
		MAPDIR+=("$(dirname "${INDEX_MAP}")")
		MAPDIR+=("${__LOG_ALTERNATES[@]}")
		for LD in "${MAPDIR[@]}" ; do
			NEW_INDEX_MAP="${LD}/${MAPNAME}"
			if __do_init_index_map "${NEW_INDEX_MAP}" ; then
				INDEX_MAP="${NEW_INDEX_MAP}"
				return 0
			fi
		done
	fi
	return 1
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
# Copy stdin to the candidate log without mirroring it on output
#
candidate_log() {
	init_candidate_log
	tee -a /dev/fd/8 &>/dev/null
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
# URL Encode the parameters given and output a parameter string
#
urlencode_params() {
	local PARAMS=""
	for p in "${@}" ; do
		case "${p}" in
			*=*	) ;;
			*	) PARAMS+="${PARAMS:+&}${p}" ; continue ;;
		esac

		# Name is everything until the first equals symbol
		local name="$(urlencode "${p%%=*}")"
		# Value is everything after that first equals symbol
		local value="$(urlencode "${p#*=}")"
		PARAMS+="${PARAMS:+&}${name}=${value}"
	done
	echo "${PARAMS}"
	return 0
}

#
# Perform a web service call using WGET
#
wget_call() {
	local METHOD="${1}"
	local URL="${2}"
	shift 2
	local PARAMS="$(urlencode_params "${@}")"
	case "${METHOD}" in
		GET		)
			"${WGET}" --tries 10 --waitretry 10 \
				--user="${ALF_USER}" --password="${ALF_PASS}" \
				-O - "${URL}${PARAMS:+?}${PARAMS}"
			;;
		POST	)
			"${WGET}" --tries 10 --waitretry 10 \
				--user="${ALF_USER}" --password="${ALF_PASS}" \
				--post-data="${PARAMS}" \
				-O - "${URL}"
			;;
		*	) say "The WGET module only supports GET and POST at this time" ; return 1 ;;
	esac
}

#
# Perform a web service call using CURL
#
curl_call() {
	local METHOD="${1}"
	local URL="${2}"
	shift 2
	local PARAMS="$(urlencode_params "${@}")"
	"${CURL}" -k -f -X "${METHOD}" -u "${ALF_USER}:${ALF_PASS}" --url "${URL}${PARAMS:+?}${PARAMS}" --retry 10 --retry-delay 10
}

#
# Perform an Alfresco web service call using either WGET or CURL, as configured
# via the CALL variable.  If not defined, it will use WGET
#
call_alfresco() {
	local ERR=""
	local RC=0
	local LOG=""
	# Capture STDERR, allow STDOUT to flow freely
	[ -f /dev/fd/9 ] && LOG="silent_log"
	[ -f /dev/fd/8 ] && LOG="candidate_log"
	if [ -n "${LOG}" ] ; then
		"${CALL:-wget_call}" "${@}" 2> >( "${LOG}" )
		RC=${?}
	else
		{
			OUT="$("${CALL:-wget_call}" "${@}" 2>&1 1>&3-)"
			RC=${?}
		} 3>&1
		[ ${RC} -ne 0 ] && say "${OUT}"
	fi
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

	local EXE="${LIBDIR}/${EXE_NAME}"
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
