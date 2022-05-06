/* -*- P4_16 -*- */
#include <core.p4>
#include <v1model.p4>

const bit<16> TYPE_IPV4 = 0x800;

/*************************************************************************
*********************** H E A D E R S  ***********************************
*************************************************************************/

typedef bit<9>  egressSpec_t;
typedef bit<48> macAddr_t;
typedef bit<32> ip4Addr_t;

header ethernet_t {
    macAddr_t dstAddr;
    macAddr_t srcAddr;
    bit<16>   etherType;
}

header ipv4_t {
    bit<4>    version;
    bit<4>    ihl;
    bit<8>    tos;
    bit<16>   totalLen;
    bit<16>   identification;
    bit<3>    flags;
    bit<13>   fragOffset;
    bit<8>    ttl;
    bit<8>    protocol;
    bit<16>   hdrChecksum;
    ip4Addr_t srcAddr;
    ip4Addr_t dstAddr;
}

struct metadata {
    int<16> rank;
}

struct headers {
    ethernet_t   ethernet;
    ipv4_t       ipv4;
}

/*************************************************************************
*********************** P A R S E R  ***********************************
*************************************************************************/

parser MyParser(packet_in packet,
                out headers hdr,
                inout metadata meta,
                inout standard_metadata_t standard_metadata) {

    state start {
        transition parse_ethernet;
    }

    state parse_ethernet {
        packet.extract(hdr.ethernet);
        transition select(hdr.ethernet.etherType) {
            TYPE_IPV4: parse_ipv4;
            default: accept;
        }
    }

    state parse_ipv4 {
        packet.extract(hdr.ipv4);
        transition accept;
    }

}

/*************************************************************************
************   C H E C K S U M    V E R I F I C A T I O N   *************
*************************************************************************/

control MyVerifyChecksum(inout headers hdr, inout metadata meta) {   
    apply {  }
}


/*************************************************************************
**************  I N G R E S S   P R O C E S S I N G   *******************
*************************************************************************/

control MyIngress(inout headers hdr,
                  inout metadata meta,
                  inout standard_metadata_t standard_metadata) {
    
    /*Queue with index 0 is the bottom one, with lowest priority*/
    register<int<32>>(8) queue_bound;
    register<int<32>>(8) queue_share_estimate;
    register<int<32>>(8) debug;

    action drop() {
        mark_to_drop(standard_metadata);
    }

    // 1 bit sign, 15 bits integer, 16 bits fractional
    action ceil(in int<32> fp, out int<16> output) {
        int<16> integer = (int<16>)(fp >> 16);
        int<16> rounding = (int<16>)(fp & 0b1000_0000_0000_0000);

        output = integer + rounding;
    }

    action min(in int<32> a, in int<32> b, out int<32> output) {
        if(a < b) { output = a; }
        else { output = b; }
    }

    action max(in int<32> a, in int<32> b, out int<32> output) {
        if(a > b) { output = a; }
        else { output = b; }
    }

    // 1 bit sign, 15 bits integer, 16 bits fractional
    action update_queue_estimate(in bit<32> queue_index, out int<32> current_estimate) {
        int<32> queue_estimate_present = 0;
        queue_share_estimate.read(queue_estimate_present, queue_index);

        // EWMA
        // alpha = 0.01, approximated by 1/128 for simplicity
        int<32> hit = (queue_index == (bit<32>)standard_metadata.priority) ? (int<32>)(1 << 16) : 0;
        current_estimate = queue_estimate_present + ((hit - queue_estimate_present) >> 7);

        queue_share_estimate.write(queue_index, current_estimate);
    }
    
    action ipv4_forward(macAddr_t dstAddr, egressSpec_t port) {
        standard_metadata.egress_spec = port;
        hdr.ethernet.srcAddr = hdr.ethernet.dstAddr;
        hdr.ethernet.dstAddr = dstAddr;
        hdr.ipv4.ttl = hdr.ipv4.ttl - 1;
    }
    
    table ipv4_lpm {
        key = {
            hdr.ipv4.dstAddr: lpm;
        }
        actions = {
            ipv4_forward;
            drop;
            NoAction;
        }
        size = 1024;
        default_action = NoAction();
    }
    
    apply {
        if (hdr.ipv4.isValid()) {
            // TODO: investigate why the top 2 bits seem to be set *only* when the first index of the bounds register is zero
            meta.rank = (int<16>)(bit<16>)hdr.ipv4.tos & 0x3F;

            // The original P4_16 SP-PIFO implementation
            // handled queues in the opposite order than the paper,
            // probably as an optimization to fuse register ops into a single stage.
            // The same order has been retained here.

            int<32> bound_0;
            int<32> bound_1;
            int<32> bound_2;
            int<32> bound_3;
            int<32> bound_4;
            int<32> bound_5;
            int<32> bound_6;

            // enqueue packet based on current bounds
            // COST: k register ops, hence k stages
            queue_bound.read(bound_0, 0);
            queue_bound.read(bound_1, 1);
            queue_bound.read(bound_2, 2);
            queue_bound.read(bound_3, 3);
            queue_bound.read(bound_4, 4);
            queue_bound.read(bound_5, 5);
            queue_bound.read(bound_6, 6);

            // Initial queue bounds,
            // in case this is the first packet
            if(bound_0 | bound_1 | bound_2 | bound_3 | bound_4 | bound_5 | bound_6 == 0) {
                bound_0 = 7 << 16;
                bound_1 = 6 << 16;
                bound_2 = 5 << 16;
                bound_3 = 4 << 16;
                bound_4 = 3 << 16;
                bound_5 = 2 << 16;
                bound_6 = 1 << 16;
            }

            int<16> current_bound = 0;
            ceil(bound_0, current_bound); // bound registers now hold fised point, convert before comparison
            if (current_bound <= meta.rank) {
                standard_metadata.priority = (bit<3>)0;
            } else {
                ceil(bound_1, current_bound);
                if (current_bound <= meta.rank) {
                    standard_metadata.priority = (bit<3>)1;
                } else {
                    ceil(bound_2, current_bound);
                    if (current_bound <= meta.rank) {
                        standard_metadata.priority = (bit<3>)2;
                    } else {
                        ceil(bound_3, current_bound);
                        if (current_bound <= meta.rank) {
                            standard_metadata.priority = (bit<3>)3;
                        } else {
                            ceil(bound_4, current_bound);
                            if (current_bound <= meta.rank) {
                                standard_metadata.priority = (bit<3>)4;
                            } else {
                                ceil(bound_5, current_bound);
                                if (current_bound <= meta.rank) {
                                    standard_metadata.priority = (bit<3>)5;
                                } else {
                                    ceil(bound_6, current_bound);
                                    if (current_bound <= meta.rank) {
                                        standard_metadata.priority = (bit<3>)6;
                                    } else {
                                        standard_metadata.priority = (bit<3>)7;
                                    }
                                }
			                }
                        }
                    }
                }
            }

            // Track each queues' share of packets
            int<32> estimate_0 = 0;
            int<32> estimate_1 = 0;
            int<32> estimate_2 = 0;
            int<32> estimate_3 = 0;
            int<32> estimate_4 = 0;
            int<32> estimate_5 = 0;
            int<32> estimate_6 = 0;
            int<32> estimate_7 = 0;

            // COST: 2*k register ops,
            // Tofino can apparently merge two consecutive read/write ops
            // into a single stage, so it may only require k stages, depending on target
            update_queue_estimate(0, estimate_0);
            update_queue_estimate(1, estimate_1);
            update_queue_estimate(2, estimate_2);
            update_queue_estimate(3, estimate_3);
            update_queue_estimate(4, estimate_4);
            update_queue_estimate(5, estimate_5);
            update_queue_estimate(6, estimate_6);
            update_queue_estimate(7, estimate_7);

            // Estimate forces between queues


            int<32> delta_f_0 = estimate_0 - estimate_1;
            int<32> delta_f_1 = estimate_1 - estimate_2;
            int<32> delta_f_2 = estimate_2 - estimate_3;
            int<32> delta_f_3 = estimate_3 - estimate_4;
            int<32> delta_f_4 = estimate_4 - estimate_5;
            int<32> delta_f_5 = estimate_5 - estimate_6;
            int<32> delta_f_6 = estimate_6 - estimate_7;

            debug.write(0, delta_f_0);
            debug.write(1, delta_f_1);
            debug.write(2, delta_f_2);
            debug.write(3, delta_f_3);
            debug.write(4, delta_f_4);
            debug.write(5, delta_f_5);
            debug.write(6, delta_f_6);

            // Compute bounds for next packet
            int<32> next_0;
            int<32> next_1;
            int<32> next_2;
            int<32> next_3;
            int<32> next_4;
            int<32> next_5;
            int<32> next_6;

            // bound_7 is always zero in this model
            min(bound_6 + delta_f_6, bound_5 - (1 << 16), next_6);
            max(next_6, (1 << 16), next_6);

            min(bound_5 + delta_f_5, bound_4 - (1 << 16), next_5);
            max(next_5, next_6 + (1 << 16), next_5);

            min(bound_4 + delta_f_4, bound_3 - (1 << 16), next_4);
            max(next_4, next_5 + (1 << 16), next_4);

            min(bound_3 + delta_f_3, bound_2 - (1 << 16), next_3);
            max(next_3, next_4 + (1 << 16), next_3);

            min(bound_2 + delta_f_2, bound_1 - (1 << 16), next_2);
            max(next_2, next_3 + (1 << 16), next_2);

            min(bound_1 + delta_f_1, bound_0 - (1 << 16), next_1);
            max(next_1, next_2 + (1 << 16), next_1);

            max(bound_0 + delta_f_0, next_1 + (1 << 16), next_0);

            // COST: k register ops, hence k stages
            // Save computed bounds
            queue_bound.write(0, next_0);
            queue_bound.write(1, next_1);
            queue_bound.write(2, next_2);
            queue_bound.write(3, next_3);
            queue_bound.write(4, next_4);
            queue_bound.write(5, next_5);
            queue_bound.write(6, next_6);

            // Forward
            ipv4_lpm.apply();
        }
    }
}

/*************************************************************************
****************  E G R E S S   P R O C E S S I N G   *******************
*************************************************************************/

control MyEgress(inout headers hdr,
                 inout metadata meta,
                 inout standard_metadata_t standard_metadata) {
    apply {
    }
}

/*************************************************************************
*************   C H E C K S U M    C O M P U T A T I O N   **************
*************************************************************************/

control MyComputeChecksum(inout headers  hdr, inout metadata meta) {
     apply {
	update_checksum(
	    hdr.ipv4.isValid(),
            { hdr.ipv4.version,
	      hdr.ipv4.ihl,
              hdr.ipv4.tos,
              hdr.ipv4.totalLen,
              hdr.ipv4.identification,
              hdr.ipv4.flags,
              hdr.ipv4.fragOffset,
              hdr.ipv4.ttl,
              hdr.ipv4.protocol,
              hdr.ipv4.srcAddr,
              hdr.ipv4.dstAddr },
            hdr.ipv4.hdrChecksum,
            HashAlgorithm.csum16);
    }
}

/*************************************************************************
***********************  D E P A R S E R  *******************************
*************************************************************************/

control MyDeparser(packet_out packet, in headers hdr) {
    apply {
        packet.emit(hdr.ethernet);
        packet.emit(hdr.ipv4);
    }
}

/*************************************************************************
***********************  S W I T C H  *******************************
*************************************************************************/

V1Switch(
    MyParser(),
    MyVerifyChecksum(),
    MyIngress(),
    MyEgress(),
    MyComputeChecksum(),
    MyDeparser()
) main;
