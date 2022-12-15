package update.ottr;
import org.apache.hadoop.util.bloom.CountingBloomFilter;

public class BloomfilterUpdate {
    CountingBloomFilter filter = new CountingBloomFilter(1000000, 5, 1);
}
