package drasouls.stackreplacetweak;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventoryManager;
import necesse.inventory.PlayerInventorySlot;
import net.bytebuddy.asm.Advice;

import java.util.Optional;
import java.util.function.Predicate;

@ModMethodPatch(target = PlayerInventoryManager.class, name = "setItem", arguments = { PlayerInventorySlot.class, InventoryItem.class })
public class PlayerInventoryManagerPatch {

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean onEnter(@Advice.This PlayerInventoryManager thiz, @Advice.Argument(0) PlayerInventorySlot slot, @Advice.Argument(1) InventoryItem invItem) {
        if (invItem != null && invItem.getAmount() == 0) {
            // find similar item in inventory
            Optional<PlayerInventorySlot> i = thiz.streamSlots(false, false, false)
                    .filter(makeFilterFunc(thiz, slot, invItem))
                    .findFirst();
            if (!i.isPresent()) return false;

            // put that item into the currently selected item
            invItem.combine(thiz.player.getLevel(), thiz.player, i.get().getItem(thiz), "replace");

            // remove the stack we got items from because we're sure we moved all of it
            if (i.get().getItem(thiz).getAmount() == 0) {
                i.get().setItem(thiz, null);
            }
            // update the inventory
            i.get().markDirty(thiz);
            slot.markDirty(thiz);
        }
        return false;
    }

    // because lambdas are not allowed within advice methods
    public static Predicate<PlayerInventorySlot> makeFilterFunc(PlayerInventoryManager thiz, PlayerInventorySlot slot, InventoryItem invItem) {
        return s -> !slot.equals(s)
                && !s.isSlotClear(thiz)
                && s.getItem(thiz) != null
                && s.getItem(thiz).getAmount() > 0
                && invItem.canCombine(thiz.player.getLevel(), thiz.player, s.getItem(thiz), "replace");
    }
}
