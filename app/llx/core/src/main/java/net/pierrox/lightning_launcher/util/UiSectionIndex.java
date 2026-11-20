package net.pierrox.lightning_launcher.util;

import android.content.ClipData;
import android.content.Context;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 白い熊: the chip index on top of the 「白い熊 雷起動盤 UI」 page — a wrapping grid of small outlined
 * chips, one per top-level section. A tap jumps (the page unfolds the section and scrolls to it); a
 * long-press lifts the chip into a drag, the chips reorder LIVE under the finger, and the page reorders
 * its sections to match on drop. Same code as 白い熊 地図 {@code ChizuSectionIndex} / kxkb
 * {@code SectionIndex}.
 */
public final class UiSectionIndex {

    public static final class Item {
        final String key;
        final CharSequence label;

        public Item(String key, CharSequence label) {
            this.key = key;
            this.label = label;
        }
    }

    public interface Listener {
        void onJump(String key);

        void onReordered(List<String> order);
    }

    private UiSectionIndex() {
    }

    public static View build(Context context, List<Item> items, int color, final Listener listener) {
        final Flow flow = new Flow(context, UiPills.dp(context, 6));
        flow.setPadding(0, UiPills.dp(context, 2), 0, UiPills.dp(context, 6));
        for (Item item : items) {
            TextView chip = UiPills.chip(context, item.label, color);
            chip.setTag(item.key);
            chip.setOnClickListener(v -> listener.onJump((String) v.getTag()));
            chip.setOnLongClickListener(v -> {
                v.setAlpha(0.3f);
                v.startDragAndDrop(ClipData.newPlainText("llui_section", (String) v.getTag()),
                        new View.DragShadowBuilder(v), v, 0);
                return true;
            });
            flow.addView(chip);
        }
        flow.setOnDragListener((host, event) -> onDrag(flow, event, listener));
        return flow;
    }

    private static boolean onDrag(Flow flow, DragEvent event, Listener listener) {
        View dragged = event.getLocalState() instanceof View ? (View) event.getLocalState() : null;
        // Another drag (an item dragged on the desktop, another app's) is not ours to answer.
        if (dragged == null || dragged.getParent() != flow) {
            return false;
        }
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_LOCATION: {
                // The drag is driven by the shadow surface, so moving the chip in the tree does not
                // cancel it — the chips reorder under the finger.
                int to = insertionIndex(flow, event.getX(), event.getY());
                int from = flow.indexOfChild(dragged);
                if (to >= 0 && to != from) {
                    try {
                        flow.removeView(dragged);
                        flow.addView(dragged, Math.min(to, flow.getChildCount()));
                    } catch (Exception ignored) {
                        // the tree was mid-change; the next location event retries
                    }
                }
                break;
            }
            case DragEvent.ACTION_DRAG_ENDED: {
                dragged.setAlpha(1f);
                final List<String> order = new ArrayList<>();
                for (int i = 0; i < flow.getChildCount(); i++) {
                    order.add((String) flow.getChildAt(i).getTag());
                }
                // Posted: the drag is still being dispatched to the chips a rebuild replaces.
                flow.post(() -> listener.onReordered(order));
                break;
            }
            default:
                break;
        }
        return true;
    }

    private static int insertionIndex(Flow flow, float x, float y) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < flow.getChildCount(); i++) {
            View ch = flow.getChildAt(i);
            if (x >= ch.getLeft() && x <= ch.getRight() && y >= ch.getTop() && y <= ch.getBottom()) {
                return i;
            }
            double d = Math.hypot(x - (ch.getLeft() + ch.getRight()) / 2.0, y - (ch.getTop() + ch.getBottom()) / 2.0);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    /** A wrapping row layout: children flow left to right and wrap to the next line. */
    static final class Flow extends ViewGroup {
        private final int gap;

        Flow(Context context, int gap) {
            super(context);
            this.gap = gap;
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int width = MeasureSpec.getSize(widthSpec);
            int inner = width - getPaddingLeft() - getPaddingRight();
            int x = 0, y = getPaddingTop(), rowH = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View ch = getChildAt(i);
                ch.measure(MeasureSpec.makeMeasureSpec(Math.max(0, inner), MeasureSpec.AT_MOST),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                int cw = ch.getMeasuredWidth(), chh = ch.getMeasuredHeight();
                if (x > 0 && x + cw > inner) {
                    x = 0;
                    y += rowH + gap;
                    rowH = 0;
                }
                x += cw + gap;
                rowH = Math.max(rowH, chh);
            }
            setMeasuredDimension(width, y + rowH + getPaddingBottom());
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int inner = getWidth() - getPaddingLeft() - getPaddingRight();
            int x = getPaddingLeft(), y = getPaddingTop(), rowH = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View ch = getChildAt(i);
                int cw = ch.getMeasuredWidth(), chh = ch.getMeasuredHeight();
                if (x > getPaddingLeft() && x + cw > getPaddingLeft() + inner) {
                    x = getPaddingLeft();
                    y += rowH + gap;
                    rowH = 0;
                }
                ch.layout(x, y, x + cw, y + chh);
                x += cw + gap;
                rowH = Math.max(rowH, chh);
            }
        }
    }
}
