package me.kitkas1412.common.cache;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TicketInventorySyncOnStartup implements CommandLineRunner {
    private final TicketInventoryReconciler ticketInventoryReconciler;

    public TicketInventorySyncOnStartup(TicketInventoryReconciler ticketInventoryReconciler) {
        this.ticketInventoryReconciler = ticketInventoryReconciler;
    }

    @Override
    public void run(String... args) throws Exception {
        ticketInventoryReconciler.reconcileAll();
    }
}
