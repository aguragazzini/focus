# Diet glance — 0.7.2

Local home line for Agus's weekday meals. Same stack as the clock, santoral, and battery glance. No widget host, no network, no streaks.

## Placement

Inside the clock item, after the battery/alarm glance and before the pause chips:

time → date → santoral → battery/alarm → diet → pause chips → Personal → Trabajo

The diet block uses the same dim centered type as the santoral detail. It is one tappable column, not a card.

## Day

Weekday comes from `America/Argentina/Cordoba`, not the device zone. The asset is `app/src/main/assets/plan_ragazzini.json`. `DietPlan.load` runs on the application background scope, same as the santoral. If the asset is missing or a weekday has no theme, the line is omitted.

Monday–Friday title: `Hoy ·` plus the fixed theme (`Día del pollo`, and so on). Two lines, `Almuerzo` and `Cena`, are the short menu, not recipes. A third line, `Desayuno`, is `Huevos y palta` on Monday, Wednesday, and Friday, and `Tortilla de verdura` on Tuesday and Thursday.

Saturday and Sunday: one line, `Fin de semana · misma base (proteína + verdura)`. No lunch or dinner on the home line.

## Sheet

Tap opens an in-app sheet.

Weekday: today's lunch, dinner, and breakfast, plus Agus's portion note when the plan has one, and up to three alternate titles from the matching recetario group. Names only.

Weekend: the three short rules (three meals, no grazing, a short walk after eating). No alternate recipes.

Kids' and Vane's carbs are not on the home line or in the sheet.

## Out of this cut

Shopping lists, prices, supplements, full recipe steps, and pantry cleanup stay in the source plan. They are not in the launcher.
