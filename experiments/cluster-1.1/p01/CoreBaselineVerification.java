import com.acme.foodordering.adapter.out.inmemory.InMemoryOrderRepository;
import com.acme.foodordering.application.port.in.PlaceOrderCommand;
import com.acme.foodordering.application.port.in.PlaceOrderResult;
import com.acme.foodordering.application.service.GetOrderService;
import com.acme.foodordering.application.service.PlaceOrderService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

public final class CoreBaselineVerification {

    public static void main(String[] args) {
        var repository = new InMemoryOrderRepository();
        var clock = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC);
        var placeOrder = new PlaceOrderService(repository, clock);
        var getOrder = new GetOrderService(repository);

        var result = placeOrder.place(new PlaceOrderCommand(
                "customer-1",
                "restaurant-1",
                List.of(new PlaceOrderCommand.Line(
                        "burger-1",
                        "Classic Burger",
                        2,
                        new BigDecimal("5.50")
                ))
        ));

        assert result instanceof PlaceOrderResult.Accepted : "expected accepted placement";
        var accepted = (PlaceOrderResult.Accepted) result;
        var placed = accepted.order();

        assert "PLACED".equals(placed.status()) : "expected PLACED";
        assert new BigDecimal("11.00").compareTo(placed.total()) == 0 : "unexpected total";
        assert repository.size() == 1 : "order was not persisted";
        assert placed.id().equals(accepted.fact().orderId().toString()) : "fact/order mismatch";

        var loaded = getOrder.get(placed.id());
        assert placed.id().equals(loaded.id()) : "loaded order differs";
        assert loaded.placedAt().equals(Instant.parse("2026-10-07T10:00:00Z")) : "unexpected timestamp";

        boolean emptyRejected = false;
        try {
            placeOrder.place(new PlaceOrderCommand(
                    "customer-1",
                    "restaurant-1",
                    List.of()
            ));
        } catch (IllegalArgumentException expected) {
            emptyRejected = true;
        }

        assert emptyRejected : "empty order should be rejected";

        System.out.println("CORE_BASELINE_VERIFICATION: PASS");
        System.out.println("orders.persisted=" + repository.size());
        System.out.println("placed.status=" + placed.status());
        System.out.println("placed.total=" + placed.total());
        System.out.println("domain.fact=" + accepted.fact().getClass().getSimpleName());
        System.out.println("durability.guaranteed=false");
        System.out.println("spring_http_path.verified=false");
    }
}
