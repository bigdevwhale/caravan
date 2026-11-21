# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
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
