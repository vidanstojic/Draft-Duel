# App content answers — Draft Duel 0.1.0

Play Console › *Policy › App content*. Answers for this build: no internet permission, no
accounts, no analytics, no ads, nothing stored beyond the running process. Update them as soon as
Firebase (spec §15), the server (spec §7) or ads (spec §16) arrive.

| Section | Answer |
|---|---|
| Privacy policy | URL of the hosted `privacy-policy.html` |
| App access | All functionality is available without special access (no login) |
| Ads | No, the app does not contain ads |
| Content rating | Questionnaire below |
| Target audience | 13–15, 16–17, 18 and over. Do **not** select an age group under 13 (that brings in the Families policy). "Unintentionally appealing to children": No |
| Data safety | Collects no data, shares no data (details below) |
| Government app | No |
| Financial features | My app doesn't provide any financial features |
| Health apps | My app does not have any health features |
| News app | No |

## Data safety

- Does your app collect or share any of the required user data types? **No**
- With "No", Play skips encryption and deletion questions. The listing will show
  "No data collected" and "No data shared with third parties".
- Internal-testing builds are exempt from showing the Data safety section, but the form must
  still be filled in before closed testing or production.

## Content rating (IARC questionnaire)

- Category: **Game**
- Violence, blood, fear, sexuality, nudity, crude humour, profanity: **No**
- Controlled substances (drugs, alcohol, tobacco): **No**
- Gambling: **No**. Players bid in-game points that cannot be bought, won or exchanged; there
  is no real money and no chance-based payout.
- Users can interact or exchange content with each other: **No** (no online play in this build)
- Shares the user's location: **No**
- Allows purchases of digital goods: **No**
- Unrestricted internet access: **No**

Expected result: PEGI 3 / ESRB Everyone or similar.
