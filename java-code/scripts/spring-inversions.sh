#!/usr/bin/env bash

RUNTIME="${RUNTIME:-1}"

NETBENCH="${NETBENCH:?unset}"
DISTRIBUTION="${TRAFFIC_DISTRIBUTION:-exponential}"

PUPD="./projects/sppifo/runs/sppifo_analysis/alternative_distributions/${DISTRIBUTION}/SPPIFO.properties"
GREEDY="./projects/sppifo/runs/sppifo_analysis/alternative_distributions/${DISTRIBUTION}/Greedy.properties"

COMMON_ARGS=("enable_rank_mapping=false" "enable_queue_bound_tracking=true" "sppifo_queuebound_log_interval=10" "run_time_s=${RUNTIME}")

spring_generate() {
    local sensitivities=(1.0)
    local alphas=(0.0001 0.001 0.01 0.08 0.1 0.2)
    local sample_modes=(COUNT_PACKETS)
    local inversion_models=(INVERSIONS_ALL INVERSIONS_QUEUE_IMMEDIATE)

    for s in "${sensitivities[@]}"; do
        for a in "${alphas[@]}"; do
            for sm in "${sample_modes[@]}"; do
                for im in "${inversion_models[@]}"; do
                    # Uses PUPD config as a base to keep base parameters the same.
                    echo "${NETBENCH[@]}" "${PUPD}" \
                        "${COMMON_ARGS[@]}" \
                        output_port_step_size=spring \
                        spring_alpha="${a}" \
                        spring_sensitivity="${s}" \
                        spring_sample_mode="${sm}" \
                        sppifo_inversion_model="${im}" \
                        run_folder_name="$(basename "${PUPD%%.properties}")-spring-a=${a}-s=${s}-sm=${sm}-im=${im}-rt=${RUNTIME}"
                done
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
        'uniform')
                STATIC_MODELS=(
                        ['upper_estimate_sum']='0,12,24,36,48,61,74,87' \
                        ['upper_estimate_per_queue_maximum']='0,12,24,36,48,61,74,87' \
                        ['exact_sum']='0,12,24,36,49,62,75,88' \
                        ['exact_per_queue_maximum']='0,12,24,36,48,61,74,87' \
                        ['inversion_count_sum']='0,1,2,3,4,5,34,67' \
                        ['inversion_count_per_queue_maximum']='0,12,24,36,48,61,74,87' \
                        ['mass_only_per_queue_maximum']='0,12,24,36,48,61,74,87' \

                )
        ;;
        'exponential')
                STATIC_MODELS=(
                        ['upper_estimate_sum']='0,6,13,20,29,40,53,71' \
                        ['upper_estimate_per_queue_maximum']='0,6,13,20,29,40,53,71' \
                        ['exact_sum']='0,6,12,19,28,38,51,69' \
                        ['exact_per_queue_maximum']='0,4,9,14,20,28,39,56' \
                        ['inversion_count_sum']='0,3,7,12,17,24,34,50' \
                        ['inversion_count_per_queue_maximum']='0,3,6,9,13,18,26,39' \
                        ['mass_only_per_queue_maximum']='0,3,7,11,16,23,32,48' \
                )
        ;;
        'inverse_exponential')
                STATIC_MODELS=(
                        ['upper_estimate_sum']='0,29,47,60,71,80,87,94' \
                        ['upper_estimate_per_queue_maximum']='0,29,47,60,71,80,87,94' \
                        ['exact_sum']='0,31,49,62,72,81,88,94' \
                        ['exact_per_queue_maximum']='0,65,74,81,86,90,94,97' \
                        ['inversion_count_sum']='0,52,68,78,85,90,94,98' \
                        ['inversion_count_per_queue_maximum']='0,79,85,89,92,95,97,99' \
                        ['mass_only_per_queue_maximum']='0,52,68,77,84,89,93,97' \
                )
        ;;
        'convex')
                STATIC_MODELS=(
                        ['upper_estimate_sum']='0,4,9,15,26,69,80,86' \
                        ['upper_estimate_per_queue_maximum']='0,5,10,17,31,72,81,87' \
                        ['exact_sum']='0,4,9,15,47,78,84,88' \
                        ['exact_per_queue_maximum']='0,3,6,10,15,29,86,89' \
                        ['inversion_count_sum']='0,3,7,12,32,89,94,97' \
                        ['inversion_count_per_queue_maximum']='0,2,4,7,11,18,94,97' \
                        ['mass_only_per_queue_maximum']='0,3,6,11,23,81,86,89' \
                )
        ;;
        #'minmax')
        #       STATIC_MODELS=(
        #               ['upper_estimate_sum']='0,7,26,32,36,39,42,46' \
        #               ['upper_estimate_per_queue_maximum']='0,7,26,32,36,39,42,46' \
        #               ['exact_sum']='0,15,30,34,37,40,43,46' \
        #               ['exact_per_queue_maximum']='0,9,35,38,40,42,44,47' \
        #               ['mass_only_per_queue_maximum']='0,29,34,37,39,41,43,46' \
        #       )
        #;;
        'poisson')
                STATIC_MODELS=(
                        ['upper_estimate_sum']='0,34,42,47,51,55,60,68' \
                        ['upper_estimate_per_queue_maximum']='0,36,42,46,50,54,59,66' \
                        ['exact_sum']='0,41,45,48,51,54,57,61' \
                        ['exact_per_queue_maximum']='0,45,48,50,52,54,57,61' \
                        ['inversion_count_sum']='0,43,46,48,50,52,55,59' \
                        ['inversion_count_per_queue_maximum']='0,45,47,49,51,53,55,58' \
                        ['mass_only_per_queue_maximum']='0,43,46,48,50,52,55,58' \
                )
        ;;
esac

for err in "${!STATIC_MODELS[@]}"; do
        echo "${NETBENCH[@]}" "${PUPD}" \
                "${COMMON_ARGS[@]}" \
                output_port_step_size=static \
                sppifo_queue_bounds="${STATIC_MODELS[$err]}" \
                run_folder_name="$(basename "${PUPD%%.properties}")-static-err=${err}-im=${im}-rt=${RUNTIME}"
done


# Spring-like heuristic with various parameters
spring_generate
