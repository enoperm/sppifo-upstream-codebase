#!/usr/bin/env bash

NETBENCH="${NETBENCH:?unset}"

PUPD="./projects/sppifo/runs/sppifo_evaluation/pFabric/data_mining_workload/37000/SPPIFO.properties"

spring_generate() {
    local sensitivities=(0.2 0.4 0.6 0.8 1.0 1.2 1.4 1.6 1.8 2.0)
    local alphas=(0.1 0.2 0.3 0.4 0.6 0.7 0.8 0.9 1.0)
    local sample_sizes=(100 1000 10000 100000)

    for s in "${sensitivities[@]}"; do
        for a in "${alphas[@]}"; do
            for ss in "${sample_sizes[@]}"; do
                echo "${NETBENCH[@]}" "${PUPD}" \
                    output_port_step_size=spring \
                    spring_alpha="${a}" \
                    spring_sensitivity="${s}" \
                    spring_sample_interval="${ss}" \
                    run_folder_name="fct-spring-a=${a}-s=${s}-ss=${ss}"
            done
        done
    done
}

# PUPD
echo "${NETBENCH[@]}" "${PUPD}"

# Spring-like heuristic with various parameters
spring_generate
