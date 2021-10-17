#!/usr/bin/env bash

NETBENCH="${NETBENCH:?unset}"
DISTRIBUTION="${TRAFFIC_DISTRIBUTION:-exponential}"

PUPD="./projects/sppifo/runs/sppifo_analysis/alternative_distributions/${DISTRIBUTION}/SPPIFO.properties"
GREEDY="./projects/sppifo/runs/sppifo_analysis/alternative_distributions/${DISTRIBUTION}/Greedy.properties"

COMMON_ARGS=("enable_rank_mapping=false" "enable_queue_bound_tracking=true" "sppifo_queuebound_log_interval=10")

spring_generate() {
    local sensitivities=(0.001 0.01 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 1.0 2.0 5.0 10.0)
    local alphas=(0.0001 0.001 0.01 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 0.99 0.999)
    local sample_modes=(COUNT_PACKETS COUNT_INVERSIONS)

    for s in "${sensitivities[@]}"; do
        for a in "${alphas[@]}"; do
            for sm in "${sample_modes[@]}"; do
                # Uses PUPD config as a base to keep base parameters the same.
                echo "${NETBENCH[@]}" "${PUPD}" \
                    "${COMMON_ARGS[@]}" \
                    output_port_step_size=spring \
                    spring_alpha="${a}" \
                    spring_sensitivity="${s}" \
                    spring_sample_mode="${sm}" \
                    run_folder_name="$(basename ${PUPD%%.properties})-spring-a=${a}-s=${s}-sm=${sm}"
            done
        done
    done
}

# Greedy
echo "${NETBENCH[@]}" "${GREEDY}" "${COMMON_ARGS[@]}"

# PUPD
echo "${NETBENCH[@]}" "${PUPD}" "${COMMON_ARGS[@]}"


# Static queue bounds from offline model

declare -A STATIC_MODELS
case "${DISTRIBUTION}" in
	'exponential')
		STATIC_MODELS=(
			['upper_estimate_sum']='0,6,13,20,29,40,53,71' \
			['upper_estimate_per_queue_maximum']='0,6,13,20,29,40,53,71' \
			['exact_sum']='0,6,12,19,28,38,51,69' \
			['exact_per_queue_maximum']='0,4,9,14,20,28,39,56' \
		)
	;;
	'inverse_exponential')
		STATIC_MODELS=(
			['upper_estimate_sum']='0,29,47,60,71,80,87,94' \
			['upper_estimate_per_queue_maximum']='0,29,47,60,71,80,87,94' \
			['exact_sum']='0,31,49,62,72,81,88,94' \
			['exact_per_queue_maximum']='0,65,74,81,86,90,94,97' \
		)
	;;
esac

for err in "${!STATIC_MODELS[@]}"; do
	echo "${NETBENCH[@]}" "${PUPD}" \
		"${COMMON_ARGS[@]}" \
		output_port_step_size=static \
		sppifo_queue_bounds="${STATIC_MODELS[$err]}" \
		run_folder_name="$(basename ${PUPD%%.properties})-static-err=${err}"
done


# Spring-like heuristic with various parameters
spring_generate
