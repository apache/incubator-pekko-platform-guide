package shopping.cart;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.Done;

public interface ItemPopularityRepository {
  CompletionStage<Done> update(String itemId, int delta);

  CompletionStage<Optional<Long>> getItem(String itemId);
}
