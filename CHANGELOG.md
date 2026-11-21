# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

## [1.0.5] - 2026-11-21

- Rides through woods, fields and the edges of town open quarters of their own (#9). Where the map's named places are kilometres apart, a ride more than 1.5 km from every quarter opens a new one on the spot. It is named after what is near – a hamlet, a station, a dacha settlement, a park, a wood or a lake, else the road – or, with nothing named around, gets a name of the wilds.

<!-- tg
Больше не придётся ездить внутри двух огромных кварталов: если поблизости нет ни одного квартала, новый появится прямо в пути
Такие кварталы называются по ближайшему посёлку, станции, лесу, озеру или дороге, а в глуши получают сказочные имена
-->

## [1.0.4] - 2026-11-21

- Quarters of the same kind are no longer copies of each other. Each one has a speciality (one of its own goods, sold cheaper), a daily import from another kind of quarter, and one good it wants above all and pays extra for (#6). A bale sold back where it was bought fetches little.
- Contracts wrap instead of being cut off; tap one to see what to do: where to buy the good, which quarter to carry it to, by when, and what comes back (#5).
- Jams are counted properly: a stop becomes a jam only if the ride moves on within 20 minutes. GPS drift of a phone lying indoors no longer adds jam time or kilometres, and a forgotten ride ends by itself again (#7).
- Dwarf tunnel curios can be found without an underground: on a ride through a forge quarter after dark (#1).

<!-- tg
Кварталы одного вида больше не одинаковые: у каждого свой фирменный товар, привоз дня и товар, за который там платят больше
Контракты видны целиком, а по тапу объясняется, где купить товар и куда его отвезти
Пробки считаются честно: стоянка дома и дрожание GPS больше не идут в зачёт
Диковины «Гномьих ходов» теперь можно найти и в городе без метро – в квартале кузниц после заката
-->

## [1.0.3] - 2026-11-21

- Fixed: when the map server was overloaded, it could answer with an empty list, and the app took that for a city with no quarters. You stayed «Beyond the city gates» for good. Such answers are now retried, and if no quarters were found, the search starts over by itself after the update.
- Towns, villages and hamlets count as quarters too, so the land between city districts and around small towns is no longer empty. Places without a Russian name show their English one instead of the local script (#3).
- Daily orders and rumours show in full instead of being cut off after one line; an unfinished order says «not yet» (#2).

<!-- tg
Исправили ошибку, из-за которой кварталы не загружались и вы всё время оказывались «за городскими воротами». После обновления они загрузятся сами
Посёлки и деревни вокруг города тоже стали кварталами, так что за городом теперь можно торговать
Поручения дня и слухи теперь видны целиком, без обрезанных строк
-->

## [1.0.2] - 2026-11-21

- Quarters load ring by ring: the nearest ones (3 km) first, then 6 and 9 km in the background. The start waits 15 seconds at most; when the map server is busy you get in at once and the quarters come later by themselves, instead of «Couldn't load the quarters».

<!-- tg
Больше не застрянете на «Не удалось загрузить кварталы»: игра начнётся, даже если сервер карт занят
Сначала подгружаются ближайшие кварталы, остальные появятся сами через минуту-другую
-->

## [1.0.1] - 2026-11-21

- Fixed: a quarter's kind was never read from OpenStreetMap – the count query always came back empty, so every quarter got a made-up kind. Kinds are now real, and quarters already met are checked again the next time you are in them.

<!-- tg
Исправили ошибку: вид квартала раньше не определялся по карте и выбирался наугад
Теперь сады, набережные и кузни настоящие, а уже знакомые кварталы перепроверятся, когда вы снова туда приедете
-->

## [1.0.0] - 2026-11-21

First release.

- Your city's real districts (OpenStreetMap) become market quarters of eight kinds, each with its own goods; buy in one, sell in another.
- Rides by car, bus, tram, underground or as a passenger; nothing to tap while driving, a steward sells marked goods on the way.
- Eight merchants with four-request stories, a trading house of ten buildings, guild orders, rumours, weekly city events, weekend fairs and a night market.
- Perishable and fragile goods, guild contracts with a deposit, stalls in other quarters, trade routes with buildings.
- A book of sixty curios found on rides, road meetings with a choice, real weather at the end of a ride.
- First steps that lead a new merchant through the game; a notification when the phone connects to your car; a quick-settings tile.
- Russian and English, light and dark themes.

<!-- tg
Новое приложение: торгуйте между кварталами своего города во время обычных поездок
Восемь купцов со своими историями, торговый дом из десяти построек и книга из шестидесяти диковин
За рулём ничего нажимать не нужно – рынок открывается на стоянке
-->
