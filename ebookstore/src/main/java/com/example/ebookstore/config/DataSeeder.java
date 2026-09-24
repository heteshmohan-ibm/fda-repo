package com.example.ebookstore.config;

import com.example.ebookstore.entity.*;
import com.example.ebookstore.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import org.springframework.context.annotation.Profile;

/**
 * Seeds the H2 in-memory database on every startup.
 * Disabled during tests (profile "test") so each test builds its own fixtures.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final BookRepository     bookRepository;
    private final UserRepository     userRepository;
    private final AddressRepository  addressRepository;
    private final CartRepository     cartRepository;
    private final PasswordEncoder    passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded — skipping.");
            return;
        }

        // ── Categories ────────────────────────────────────────────────────────
        Category fiction     = save(Category.builder().name("Fiction").build());
        Category nonFiction  = save(Category.builder().name("Non-Fiction").build());
        Category science     = save(Category.builder().name("Science & Technology").build());
        Category selfHelp    = save(Category.builder().name("Self-Help").build());
        Category history     = save(Category.builder().name("History").build());

        // ── Books ─────────────────────────────────────────────────────────────
        // Fiction
        book("The Great Gatsby",         "F. Scott Fitzgerald", "Scribner",
             "A novel of the Jazz Age.",              new BigDecimal("299.00"), 25, fiction);
        book("To Kill a Mockingbird",    "Harper Lee",          "HarperCollins",
             "A story of racial injustice.",          new BigDecimal("349.00"), 18, fiction);
        book("1984",                     "George Orwell",       "Secker & Warburg",
             "Dystopian social science fiction.",     new BigDecimal("279.00"), 30, fiction);
        book("Pride and Prejudice",      "Jane Austen",         "T. Egerton",
             "A romantic novel of manners.",          new BigDecimal("199.00"), 22, fiction);
        book("The Alchemist",            "Paulo Coelho",        "HarperOne",
             "A philosophical novel.",                new BigDecimal("399.00"), 15, fiction);

        // Non-Fiction
        book("The Joy of Minimalism",    "Rachel Green",        "ABC Publishers",
             "The Joy of Minimalism guides you through practical strategies to declutter your mind, space, and workflow with a calm, mindful approach.", new BigDecimal("149.00"), 42, nonFiction);
        book("Mastering Simplicity",     "Elena Vance",         "HarperCollins",
             "Discover how trimming cognitive clutter leads to monumental leaps in mental clarity and creative execution.", new BigDecimal("199.00"), 30, nonFiction);
        book("Sapiens",                  "Yuval Noah Harari",   "Harvill Secker",
             "A brief history of humankind.",         new BigDecimal("499.00"), 20, nonFiction);
        book("Educated",                 "Tara Westover",       "Random House",
             "A memoir about family and education.",  new BigDecimal("449.00"), 12, nonFiction);
        book("The Body",                 "Bill Bryson",         "Doubleday",
             "A guide for occupants of the body.",    new BigDecimal("399.00"), 14, nonFiction);

        // Science & Technology
        book("A Brief History of Time",  "Stephen Hawking",     "Bantam Books",
             "The universe and its origins.",         new BigDecimal("349.00"), 17, science);
        book("The Pragmatic Programmer", "Andrew Hunt",         "Addison-Wesley",
             "Your journey to mastery.",              new BigDecimal("599.00"), 10, science);
        book("Clean Code",               "Robert C. Martin",    "Prentice Hall",
             "A handbook of agile software craftsmanship.", new BigDecimal("649.00"), 8, science);
        book("Design Patterns",          "Gang of Four",        "Addison-Wesley",
             "Elements of reusable object-oriented software.", new BigDecimal("699.00"), 6, science);

        // Self-Help
        book("The Path to Success",      "Michael Scott",       "Penguin Random House",
             "A pragmatic blueprint for achieving enduring personal and professional breakthroughs through discipline and focus.", new BigDecimal("359.00"), 28, selfHelp);
        book("The Art of Focus",         "David Kim",           "Bloomsbury",
             "Master the science of deep concentration, overcome digital distractions, and reclaim your flow in demanding creative fields.", new BigDecimal("399.00"), 19, selfHelp);
        book("Atomic Habits",            "James Clear",         "Avery",
             "An easy and proven way to build good habits.", new BigDecimal("449.00"), 35, selfHelp);
        book("Think and Grow Rich",      "Napoleon Hill",       "Ralston Society",
             "Principles of personal achievement.",   new BigDecimal("299.00"), 20, selfHelp);
        book("The 7 Habits",             "Stephen R. Covey",    "Free Press",
             "Highly effective people.",              new BigDecimal("399.00"), 18, selfHelp);

        // History
        book("Guns, Germs, and Steel",   "Jared Diamond",       "W. W. Norton",
             "Fates of human societies.",             new BigDecimal("499.00"), 11, history);
        book("The Silk Roads",           "Peter Frankopan",     "Bloomsbury",
             "A new history of the world.",           new BigDecimal("549.00"), 9,  history);

        // ── Demo user (id will be 1) ───────────────────────────────────────────
        User demo = userRepository.save(
                User.builder()
                        .name("Demo User")
                        .email("demo@ebookstore.com")
                        .password(passwordEncoder.encode("demo1234"))
                        .giftPointsBalance(500)
                        .build());

        // ── Addresses ─────────────────────────────────────────────────────────
        Address home = addressRepository.save(
                Address.builder()
                        .user(demo)
                        .recipient("Demo User")
                        .line1("12 MG Road")
                        .line2("Apt 4B")
                        .city("Bengaluru")
                        .state("Karnataka")
                        .postalCode("560001")
                        .build());

        addressRepository.save(
                Address.builder()
                        .user(demo)
                        .recipient("Demo User")
                        .line1("42 Anna Salai")
                        .city("Chennai")
                        .state("Tamil Nadu")
                        .postalCode("600002")
                        .build());

        // ── Cart (empty, ready to use) ────────────────────────────────────────
        cartRepository.save(Cart.builder().user(demo).build());

        log.info("Database seeded: {} categories, {} books, 1 demo user (email=demo@ebookstore.com, password=demo1234, points=500), 2 addresses.",
                categoryRepository.count(), bookRepository.count());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Category save(Category c) {
        return categoryRepository.save(c);
    }

    private void book(String title, String author, String publisher,
                      String description, BigDecimal price,
                      int stock, Category category) {
        bookRepository.save(
                Book.builder()
                        .title(title)
                        .author(author)
                        .publisher(publisher)
                        .description(description)
                        .price(price)
                        .availableStock(stock)
                        .category(category)
                        .build());
    }
}
