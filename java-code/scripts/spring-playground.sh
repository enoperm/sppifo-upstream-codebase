#!/usr/bin/env bash

NETBENCH="${NETBENCH:?unset}"

PUPD="./projects/sppifo/runs/sppifo_analysis/alternative_distributions/exponential/SPPIFO.properties"
GREEDY="./projects/sppifo/runs/sppifo_analysis/alternative_distributions/exponential/Greedy.properties"

spring_generate() {
    local sensitivities=(0.2 0.4 0.6 0.8 1.0 1.5 2.0)
    local alphas=(0.2 0.4 0.6 0.8 1.0)
    local sample_sizes=(100 1000 10000 100000)

    for s in "${sensitivities[@]}"; do
        for a in "${alphas[@]}"; do
            for ss in "${sample_sizes[@]}"; do
                echo "${NETBENCH[@]}" "${PUPD}" \
                    output_port_step_size=spring \
                    spring_alpha="${a}" \
                    spring_sensitivity="${s}" \
                    spring_sample_interval="${ss}" \
                    run_folder_name="$(basename ${PUPD%%.properties})-spring-a=${a}-s=${s}-ss=${ss}"
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
