package kr.clutch.gacha.listener;

import kr.clutch.gacha.service.GachaService;
import kr.clutch.gacha.service.TicketService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class TicketInteractListener implements Listener {
    private final TicketService ticketService;
    private final GachaService gachaService;

    public TicketInteractListener(TicketService ticketService, GachaService gachaService) {
        this.ticketService = ticketService;
        this.gachaService = gachaService;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack itemStack = event.getItem();
        if (!ticketService.isTicket(itemStack)) {
            return;
        }
        event.setCancelled(true);
        if (!gachaService.canRollDefaultBox(event.getPlayer())) {
            return;
        }
        ticketService.consumeHeldTicket(event.getPlayer());
        gachaService.rollDefaultBox(event.getPlayer());
    }
}
