# Foco 0.9.9

versionName `0.9.9`, versionCode `24`. Same `signing/foco-debug.keystore`.

## Page swipe

A horizontal move that starts on an app tile used to be swallowed until it was either large or old enough to become a group drag. The pager then settled late, or not at all. Before the short arm time, any move past slop is now the pager or the list and is not consumed. A finger that stays still through that arm can still drag to group.

The pager keeps one page on each side composed, and the snap spring is stiffer with no bounce.
