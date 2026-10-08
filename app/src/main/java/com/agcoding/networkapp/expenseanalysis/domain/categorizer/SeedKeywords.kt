package com.agcoding.networkapp.expenseanalysis.domain.categorizer

import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.DINING_LEISURE
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.EDUCATION
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.FINANCE
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.GROCERIES
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.HEALTH
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.HOUSING
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.KIDS_PETS
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.SHOPPING_PERSONAL
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.SUBSCRIPTIONS
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.TRANSPORT
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.TRAVEL

/**
 * A word (or phrase) that hints at a category.
 *
 * @param parts normalised tokens; more than one means they must appear next to each other
 * @param exact match whole words only, no stems or typos (for short or ambiguous words)
 */
internal class SeedKeyword(
    val parts: List<String>,
    val category: ExpenseCategory,
    val weight: Double,
    val exact: Boolean,
)

/**
 * Built-in dictionary the categoriser starts from, in Greek, English and Greeklish.
 * Longer words also match as stems ("ασφαλ" → "ασφάλεια", "ασφαλιστική") and with small typos.
 * A leading "=" marks a word that must match exactly ("=ταξί" must not match "ταξίδι").
 */
internal object SeedKeywords {

    private const val BRAND  = 3.0
    private const val STRONG = 2.5
    private const val MEDIUM = 2.0
    private const val WEAK   = 1.0

    val all: List<SeedKeyword> by lazy {
        buildList {
            // ── Subscriptions: streaming, software, AI tools, gym, cards ──────────
            add(SUBSCRIPTIONS, BRAND,
                "netflix", "νετφλιξ", "spotify", "σποτιφαι", "disney", "hbo", "hbo max", "amazon prime",
                "prime video", "youtube premium", "youtube music", "apple music", "apple tv", "apple one",
                "icloud", "google one", "google drive", "dropbox", "onedrive", "microsoft 365", "office 365",
                "adobe", "photoshop", "lightroom", "canva", "notion", "evernote", "chatgpt", "openai", "claude",
                "anthropic", "gemini", "copilot", "github", "midjourney", "cursor", "perplexity", "grammarly",
                "playstation", "ps plus", "xbox", "game pass", "nintendo", "steam", "twitch", "patreon",
                "deezer", "tidal", "audible", "kindle unlimited", "storytel", "duolingo", "ertflix",
                "cosmote tv", "vodafone tv", "nova tv", "linkedin premium", "1password", "lastpass",
                "nordvpn", "expressvpn", "surfshark", "revolut plus", "revolut premium", "revolut metal",
                "revolut ultra", "n26 smart", "n26 metal",
                "gym", "γυμναστηριο", "fitness", "crossfit", "pilates", "πιλατες", "yoga", "γιογκα",
                "holmes place", "anytime fitness", "beat gym",
            )
            add(SUBSCRIPTIONS, STRONG,
                "συνδρομη", "συνδρομες", "subscription", "membership", "streaming", "vpn", "cloud",
                "ai tool", "ai tools", "premium", "nova", "γυμναστικη",
            )
            add(SUBSCRIPTIONS, MEDIUM, "=card", "cards", "καρτα", "καρτες", "=plus", "=pro", "apps", "app store", "google play")
            add(SUBSCRIPTIONS, WEAK, "=apple", "=google", "=microsoft", "=amazon", "=ai", "=tv", "μηνιαια συνδρομη")

            // ── Home & bills: rent, utilities, internet, phone, common charges ────
            add(HOUSING, BRAND,
                "ενοικιο", "ενοικια", "rent", "mortgage", "στεγαστικο", "ρευμα", "ηλεκτρικο ρευμα",
                "ηλεκτρισμος", "electricity", "electric bill", "=δεη", "protergia", "heron", "elpedison",
                "=nrg", "watt volt", "zenith", "φυσικο αεριο", "natural gas", "νερο", "water", "=ευδαπ",
                "=ευαθ", "=δευα", "υδρευση", "internet", "ιντερνετ", "wifi", "broadband", "fiber", "οπτικη ινα",
                "κοινοχρηστα", "common charges", "θερμανση", "heating", "πετρελαιο θερμανσης", "heating oil",
                "ασφαλεια σπιτιου", "home insurance", "house insurance", "=ενφια", "δημοτικα τελη",
                "cell phone", "mobile phone", "κινητο τηλεφωνο", "σταθερο τηλεφωνο", "phone bill",
            )
            add(HOUSING, STRONG,
                "house", "σπιτι", "home", "apartment", "διαμερισμα", "κατοικια", "τηλεφωνο", "phone",
                "κινητο", "mobile", "=cell", "cosmote", "vodafone", "=wind", "inalan", "=οτε", "=ote",
                "καθαριστρια", "cleaning", "καθαρισμος", "συναγερμος", "alarm", "verisure", "=ikea",
                "ηλεκτρικ", "utilities", "=gas", "landlord", "σπιτιου",
            )
            add(HOUSING, WEAK, "bill", "bills", "λογαριασμος", "λογαριασμοι", "επιπλα", "furniture")

            // ── Transport & car: fuel, service, insurance, parking, tolls ─────────
            add(TRANSPORT, BRAND,
                "βενζινη", "benzini", "βενζινες", "fuel", "petrol", "gasoline", "καυσιμα", "diesel",
                "gas station", "=shell", "=bp", "=eko", "=εκο", "avin", "revoil", "=elin", "cyclon",
                "σερβις", "service αυτοκινητου", "car service", "συνεργειο", "mechanic", "λαστιχα", "ελαστικα",
                "tires", "tyres", "ασφαλεια αυτοκινητου", "ασφαλεια αμαξιου", "ασφαλεια μηχανης",
                "car insurance", "auto insurance", "motorcycle insurance", "τελη κυκλοφοριας", "road tax",
                "=κτεο", "=kteo", "διοδια", "tolls", "=toll", "αττικη οδος", "attiki odos", "εγνατια",
                "parking", "παρκινγκ", "παρκιν", "σταθμευση", "garage", "γκαραζ", "=taxi", "=ταξι", "uber",
                "freenow", "free now", "=bolt", "=beat", "μετρο", "metro", "λεωφορειο", "=bus", "=οασα",
                "=ktel", "=κτελ", "epass", "leasing", "car loan", "δοση αυτοκινητου", "car wash", "πλυντηριο αυτοκινητου",
            )
            add(TRANSPORT, STRONG,
                "αυτοκινητο", "αυτοκινητου", "αμαξι", "αμαξιου", "=car", "cars", "μηχανη", "μοτοσυκλετα",
                "motorcycle", "=moto", "scooter", "πατινι", "transport", "μετακινηση", "μετακινησεις",
                "εισιτηρια μετρο", "καρτα απεριοριστων", "πετρελαιο", "=lime", "service",
            )

            // ── Dining out & leisure ──────────────────────────────────────────────
            add(DINING_LEISURE, BRAND,
                "εστιατοριο", "restaurant", "ταβερνα", "delivery", "ντελιβερι", "efood", "=wolt", "=box",
                "καφες", "καφε", "coffee", "cafe", "καφετερια", "starbucks", "coffee island", "=μπαρ", "=bar",
                "bars", "ποτο", "ποτα", "drinks", "=club", "clubs", "σινεμα", "cinema", "movies", "θεατρο",
                "theater", "theatre", "συναυλια", "concert", "festival", "διασκεδαση", "entertainment",
                "φαγητο εξω", "eating out", "dining", "εξοδος", "εξοδοι", "nightlife", "μπυρα", "beer",
                "μπυρες", "beers", "πιτσα", "pizza", "σουβλακι", "souvlaki", "burger", "sushi", "brunch", "goodys", "mcdonald",
                "everest", "=kfc", "bowling", "escape room", "ελευθερος χρονος", "χαλαρωση", "free time",
            )
            add(DINING_LEISURE, STRONG,
                "φαγητο", "food", "lunch", "dinner", "δειπνο", "βολτα", "βολτες", "hobby", "χομπι",
                "χομπυ", "fun", "leisure", "games", "παιχνιδια", "events", "εκδηλωση", "εκδηλωσεις",
            )

            // ── Travel ────────────────────────────────────────────────────────────
            add(TRAVEL, BRAND,
                "ταξιδι", "ταξιδια", "travel", "trip", "trips", "διακοπες", "vacation", "holiday", "holidays",
                "ξενοδοχειο", "ξενοδοχεια", "hotel", "hotels", "airbnb", "booking", "πτηση", "πτησεις", "flight", "flights",
                "αεροπορικα", "αεροπορικο", "airline", "aegean", "ryanair", "sky express", "easyjet", "wizz",
                "πλοιο", "ferry", "ακτοπλοικα", "blue star", "seajets", "minoan", "=anek", "rent a car",
                "car rental", "ενοικιαση αυτοκινητου", "resort", "camping", "κατασκηνωση",
            )
            add(TRAVEL, STRONG, "=τρενο", "train", "hellenic train", "εισιτηρια", "tickets", "αεροπλανο", "=βιζα", "visa")

            // ── Groceries ─────────────────────────────────────────────────────────
            add(GROCERIES, BRAND,
                "σουπερ μαρκετ", "σουπερμαρκετ", "supermarket", "super market", "groceries", "grocery",
                "τροφιμα", "σκλαβενιτης", "sklavenitis", "βασιλοπουλος", "vasilopoulos", "=lidl", "μασουτης",
                "masoutis", "my market", "κρητικος", "γαλαξιας", "bazaar", "market in", "carrefour", "=aldi",
                "φουρνος", "bakery", "λαικη", "χασαπης", "butcher", "μαναβης", "κρεοπωλειο",
            )
            add(GROCERIES, STRONG, "ψωνια σπιτιου", "ψωνια", "=σουπερ", "=super", "market", "μαρκετ")

            // ── Health ────────────────────────────────────────────────────────────
            add(HEALTH, BRAND,
                "γιατρος", "γιατρου", "doctor", "οδοντιατρος", "dentist", "φαρμακειο", "pharmacy", "φαρμακα",
                "medicine", "medication", "νοσοκομειο", "hospital", "κλινικη", "clinic", "εξετασεις",
                "ψυχολογος", "ψυχοθεραπεια", "therapy", "therapist", "φυσικοθεραπεια", "physio",
                "ασφαλεια υγειας", "health insurance", "ιδιωτικη ασφαλεια", "medical", "ιατρικ", "οπτικα",
                "γυαλια", "φακοι επαφης", "contact lenses", "βιταμινες", "vitamins", "συμπληρωματα",
                "supplements", "διατροφολογος", "dietitian", "ορθοδοντικ",
            )
            add(HEALTH, STRONG, "υγεια", "health", "interamerican", "eurolife", "θεραπεια")

            // ── Banking, loans & taxes ────────────────────────────────────────────
            add(FINANCE, BRAND,
                "bank fees", "τραπεζα", "τραπεζικα", "bank", "banking", "προμηθεια", "προμηθειες",
                "χρεωσεις τραπεζας", "τοκοι", "interest", "δανειο", "δανεια", "loan", "loans",
                "καταναλωτικο", "πιστωτικη", "πιστωτικης", "credit card", "revolut", "=n26", "eurobank",
                "πειραιως", "piraeus", "alpha bank", "εθνικη τραπεζα", "=nbg", "paypal", "φορος", "φορου",
                "φοροι", "=tax", "taxes", "εφορια", "=εφκα", "=efka", "=τεβε", "=ικα", "εισφορες",
                "contributions", "λογιστης", "accountant", "ασφαλεια ζωης", "life insurance",
                "συνταξιοδοτικο", "pension", "επενδυση", "επενδυσεις", "investment", "investing",
            )
            add(FINANCE, STRONG, "fees", "fee", "χρεωσεις", "δοση", "δοσεις", "installment", "=viva", "=etf")
            add(FINANCE, WEAK, "ασφαλεια", "ασφαλιστρα", "insurance", "ασφαλιστικη")

            // ── Education ─────────────────────────────────────────────────────────
            add(EDUCATION, BRAND,
                "φροντιστηριο", "μαθηματα", "lessons", "course", "courses", "udemy", "coursera", "σχολη",
                "σχολειο", "school", "πανεπιστημιο", "university", "διδακτρα", "tuition", "βιβλια", "books",
                "αγγλικα", "english lessons", "ξενες γλωσσες", "σεμιναριο", "seminar", "masterclass",
                "μεταπτυχιακο", "masters", "certification", "πιστοποιηση",
            )
            add(EDUCATION, STRONG, "εκπαιδευση", "education", "training", "learning")

            // ── Shopping & personal care ──────────────────────────────────────────
            add(SHOPPING_PERSONAL, BRAND,
                "ρουχα", "clothes", "clothing", "παπουτσια", "shoes", "κομμωτηριο", "κουρειο", "κουρεμα",
                "hairdresser", "barber", "haircut", "καλλυντικα", "cosmetics", "hondos", "sephora", "=zara",
                "nails", "νυχια", "μανικιουρ", "manicure", "=spa", "μασαζ", "massage", "αισθητικος",
                "αποτριχωση", "skroutz", "shein", "=temu", "δωρα", "δωρο", "gifts", "=gift",
                "ηλεκτρονικ", "electronics", "=public", "κωτσοβολος", "πλαισιο", "shopping",
            )
            add(SHOPPING_PERSONAL, STRONG, "αγορες", "purchases", "προσωπικα", "personal care", "beauty", "ομορφια")

            // ── Kids & pets ───────────────────────────────────────────────────────
            add(KIDS_PETS, BRAND,
                "παιδικος σταθμος", "νηπιαγωγειο", "kindergarten", "daycare", "νταντα", "babysitter", "πανες", "diapers", "κατοικιδιο",
                "κατοικιδια", "=pet", "pets", "σκυλος", "σκυλου", "=dog", "dogs", "γατα", "γατας", "=cat",
                "cats", "κτηνιατρος", "=vet", "veterinary", "pet food", "τροφη σκυλου", "τροφη γατας",
            )
            add(KIDS_PETS, STRONG, "παιδι", "παιδια", "παιδιου", "kids", "child", "children", "baby", "μωρο")
        }
    }

    private fun MutableList<SeedKeyword>.add(category: ExpenseCategory, weight: Double, vararg words: String) {
        words.forEach { word ->
            val exact = word.startsWith("=")
            val parts = TextNormalizer.tokens(word.removePrefix("="))
            if (parts.isNotEmpty()) add(SeedKeyword(parts, category, weight, exact))
        }
    }
}
