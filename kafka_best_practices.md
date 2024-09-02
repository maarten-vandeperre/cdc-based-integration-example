# Kafka Topics: Best Practices for Partitions and Replicas

## 1. Choosing the Number of Partitions

- **Throughput and Parallelism**: Partitions enable parallelism in Kafka. The more partitions a topic has, the more consumers can process the data in parallel, increasing throughput. However, having too many partitions can lead to increased overhead and complexity.
- **Rule of Thumb**: Start with a reasonable number of partitions (e.g., 2–4 times the number of expected consumers). Over-provisioning partitions can be beneficial for future scalability, but avoid creating too many.
- **Scaling Consideration**: Partitions cannot be reduced once created. Plan for future scalability by provisioning enough partitions initially.
- **Producer Consideration**: Producers distribute messages across partitions, and the number of partitions affects producer performance. Ensure the partition count aligns with the producer’s capability to handle multiple partitions.
- **Latency Impact**: A higher number of partitions can increase the time it takes for Kafka to elect a leader during failover, potentially impacting latency.

## 2. Replication Factor

- **Fault Tolerance**: The replication factor determines how many copies of each partition are stored in the cluster. A higher replication factor increases fault tolerance by ensuring that more replicas of data are available in case of broker failures.
- **Best Practice**: Typically, set the replication factor to 3. This ensures that your data can survive the failure of one broker and still maintain a quorum. For critical data, consider higher replication.
- **Cluster Size Consideration**: Ensure the replication factor is less than or equal to the number of brokers in your cluster. Otherwise, some replicas will be placed on the same broker, which reduces redundancy.

## 3. Partition and Replica Placement

- **Data Locality**: Kafka automatically distributes partitions across brokers to balance the load. Ensure partitions are evenly distributed to avoid hotspots where a few brokers handle a disproportionate load.
- **Rack Awareness**: Use rack-aware replication to ensure replicas are placed on different racks or availability zones. This reduces the risk of data loss if an entire rack or zone fails. Configure this using `broker.rack` and enable rack-aware replica assignment.
- **Avoid Co-Located Replicas**: Ensure that replicas of the same partition are not placed on the same broker to avoid single points of failure.

## 4. Monitoring and Adjusting Partition and Replica Configuration

- **Monitor Lag and Throughput**: Use Kafka monitoring tools to track consumer lag and producer throughput. If you notice increased lag or bottlenecks, consider adding more partitions.
- **Rebalance Partitions**: As your cluster grows or data patterns change, consider rebalancing partitions across brokers to ensure even distribution of load. Kafka’s `kafka-reassign-partitions.sh` tool can help with this.
- **Adjusting Partitions**: If your topic’s partitions are under- or over-utilized, you may need to adjust the number of partitions. Increasing partitions is possible but requires careful planning to avoid consumer rebalancing issues.

## 5. Replication Trade-Offs

- **Higher Replication Factor**: Increases durability and fault tolerance but at the cost of higher disk space usage and increased network traffic. It also leads to higher write latency as data needs to be acknowledged by more brokers.
- **Lower Replication Factor**: Reduces storage and network overhead but increases the risk of data loss and reduces availability during broker failures.

## 6. Replication and Acknowledgement Settings

- **Acknowledgement Settings**: Producers can configure `acks` to control how many replicas must acknowledge a write before it is considered successful:
   - `acks=0`: No acknowledgment is required, leading to higher throughput but lower durability.
   - `acks=1`: Only the leader partition must acknowledge the write, balancing durability and performance.
   - `acks=all`: All in-sync replicas must acknowledge the write, ensuring the highest durability at the cost of increased latency.
- **In-Sync Replicas (ISR)**: Ensure that the number of `min.insync.replicas` is set appropriately (usually `replication.factor - 1`). This setting dictates the minimum number of replicas that must acknowledge a write for it to be successful, preventing potential data loss if too few replicas are available.

## 7. Topic Retention and Compaction

- **Retention Policy**: Configure the retention period (`retention.ms`) based on how long you need to keep the data. For use cases where data must be retained indefinitely, consider setting `retention.ms` to a very large value.
- **Log Compaction**: For topics where only the latest value of a key matters (e.g., changelog streams), enable log compaction. This helps reduce storage usage by retaining only the most recent message for each key.

## 8. Best Practices for Production

- **Avoid Too Few Partitions**: Having too few partitions can lead to underutilization of resources and limited parallelism, bottlenecking the performance.
- **Avoid Too Many Partitions**: Excessive partitions can lead to higher memory usage on brokers, increased load during leader elections, and more complex operations management. Typically, avoid exceeding 10,000 partitions per broker.
- **Test Configuration**: Always test partition and replication configurations in a staging environment to understand their impact on performance and reliability before deploying to production.

## 9. Documentation and Governance

- **Document Partitioning Strategy**: Clearly document the partitioning strategy and the rationale behind the number of partitions and replicas. This helps in maintaining and scaling the Kafka cluster as the system evolves.
- **Governance**: Implement governance to review topic creation requests, ensuring appropriate partition and replication settings based on the use case.
