#!/usr/bin/env bash

NETBENCH="${NETBENCH:?unset}"

PUPD="./projects/sppifo/runs/sppifo_analysis/alternative_distributions/exponential/SPPIFO.properties"
GREEDY="./projects/sppifo/runs/sppifo_analysis/alternative_distributions/exponential/Greedy.properties"

spring_generate() {
    local sensitivities=(0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 1.0 2.0 5.0 10.0 20.0 50.0 100.0)
    local alphas=(0.2 0.4 0.6 0.8)
    local sample_modes=(COUNT_PACKETS COUNT_INVERSIONS)

    for s in "${sensitivities[@]}"; do
        for a in "${alphas[@]}"; do
            for sm in "${sample_modes[@]}"; do
                echo "${NETBENCH[@]}" "${PUPD}" \
                    output_port_step_size=spring \
                    spring_alpha="${a}" \
                    spring_sensitivity="${s}" \
                    spring_sample_mode="${sm}" \
                    run_folder_name="$(basename ${PUPD%%.properties})-spring-a=${a}-s=${s}-sm=${sm}" \
                    enable_rank_mapping=true \
                    enable_queue_bound_tracking=true
            done
        done
    done
}

# PUPD
echo "${NETBENCH[@]}" "${PUPD}"

# Greedy
echo "${NETBENCH[@]}" "${GREEDY}"

# Spring-like heuristic with various parameters
spring_generate
