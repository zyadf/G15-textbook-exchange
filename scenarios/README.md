# Scenario packs

Your group works ONLY with the folder of your scenario (run the GroupSeeder to see which one).

| Folder | Scenario | data/ (JSON) | docs/ (RAG) |
|--------|----------|--------------|-------------|
| s1-kopitiam | Kopitiam Ordering Agent | menu.json | handbook, faq, promotions |
| s2-makerspace | Makerspace Equipment Loans | equipment.json, members.json, loans.json (seed) | safety-policy, access-and-loans, equipment-care |
| s3-rooms | Campus Room Booking | rooms.json, bookings.json (seed) | room-policy, building-guide, av-equipment |
| s4-grooming | Pet Grooming Scheduler | services.json, breeds.json, pets.json, groomers.json, appointments.json (seed) | grooming-guide, care-policy, pricing-faq |
| s5-textbooks | Textbook Exchange | books.json, listings.json (seed) | exchange-rules, condition-grading, buyer-faq |

Rules for the data:
* Treat `data/` as READ-ONLY seed data. Files your tools change must be copied to `runtime/`
  on first start (see `JsonStore.seeded`). Deleting a `runtime/` file resets it.
* Money is stored in sen (`priceSen`, `askingSen` ...). Never use double for money.
* All people, IDs, e-mail addresses and phone numbers are synthetic test data.
  ISBNs in s5 are synthetic.
* You may ADD records (justify them in your design report). Do not change or delete existing records:
  the tutor's marking cases depend on them.
