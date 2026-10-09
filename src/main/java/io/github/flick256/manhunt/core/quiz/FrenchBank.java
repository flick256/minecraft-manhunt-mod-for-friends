package io.github.flick256.manhunt.core.quiz;

import static io.github.flick256.manhunt.core.quiz.Question.mcq;

import java.util.ArrayList;
import java.util.List;

/** Easy VCE French (Year 12) questions: vocabulary, core verbs, past and near future tenses, grammar basics. */
public final class FrenchBank {
    private FrenchBank() {}

    public static List<Question> questions() {
        List<Question> q = new ArrayList<>();
        Topic t = Topic.FRENCH;

        // Family
        q.add(mcq(t, "What does \"la mère\" mean?", "mother", "father", "aunt", "sister"));
        q.add(mcq(t, "What does \"le frère\" mean?", "brother", "father", "cousin", "son"));
        q.add(mcq(t, "What does \"la sœur\" mean?", "sister", "mother", "aunt", "niece"));

        // Food and drink
        q.add(mcq(t, "What does \"le pain\" mean?", "bread", "milk", "cheese", "butter"));
        q.add(mcq(t, "What does \"le lait\" mean?", "milk", "water", "juice", "coffee"));
        q.add(mcq(t, "How do you say 'the apple' in French?", "la pomme", "la poire", "la banane", "la fraise"));
        q.add(mcq(t, "What does \"le fromage\" mean?", "cheese", "butter", "egg", "apple"));

        // School
        q.add(mcq(t, "What does \"l'école\" mean?", "school", "hospital", "market", "shop"));
        q.add(mcq(t, "What does \"le professeur\" mean?", "teacher", "doctor", "driver", "farmer"));
        q.add(mcq(t, "What does \"le cahier\" mean?", "exercise book", "pencil", "bag", "ruler"));
        q.add(mcq(t, "How do you say 'the pen' in French?", "le stylo", "le crayon", "la règle", "la gomme"));

        // Home
        q.add(mcq(t, "What does \"la cuisine\" mean?", "kitchen", "bathroom", "bedroom", "garden"));
        q.add(mcq(t, "How do you say 'the window' in French?", "la fenêtre", "la porte", "le mur", "le toit"));
        q.add(mcq(t, "What does \"le lit\" mean?", "bed", "chair", "table", "sofa"));

        // Body
        q.add(mcq(t, "What does \"la main\" mean?", "hand", "foot", "head", "nose"));
        q.add(mcq(t, "How do you say 'the head' in French?", "la tête", "le bras", "la jambe", "le nez"));
        q.add(mcq(t, "What does \"les yeux\" mean?", "eyes", "ears", "teeth", "hair"));

        // Weather
        q.add(mcq(t, "What does \"il pleut\" mean?", "it is raining", "it is snowing", "it is cold", "it is hot"));
        q.add(mcq(t, "What does \"il fait chaud\" mean?", "it is hot", "it is cold", "it is windy", "it is raining"));

        // Colours
        q.add(mcq(t, "What does \"vert\" mean?", "green", "red", "blue", "yellow"));
        q.add(mcq(t, "How do you say 'black' in French?", "noir", "blanc", "gris", "marron"));

        // Numbers
        q.add(mcq(t, "What is \"dix\" in English?", "ten", "nine", "six", "twelve"));
        q.add(mcq(t, "How do you say 'five' in French?", "cinq", "quatre", "sept", "trois"));
        q.add(mcq(t, "What does \"vingt\" mean?", "twenty", "twelve", "thirty", "two"));
        q.add(mcq(t, "How do you say 'one hundred' in French?", "cent", "mille", "dix", "vingt"));

        // Days and time
        q.add(mcq(t, "What does \"lundi\" mean?", "Monday", "Friday", "Sunday", "Tuesday"));
        q.add(mcq(t, "How do you say 'Wednesday' in French?", "mercredi", "jeudi", "samedi", "mardi"));
        q.add(mcq(t, "What does \"dimanche\" mean?", "Sunday", "Saturday", "Monday", "Thursday"));
        q.add(mcq(t, "What does \"hier\" mean?", "yesterday", "tomorrow", "today", "last week"));
        q.add(mcq(t, "What does \"demain\" mean?", "tomorrow", "yesterday", "today", "tonight"));
        q.add(mcq(t, "What does \"aujourd'hui\" mean?", "today", "yesterday", "tomorrow", "now"));

        // Months
        q.add(mcq(t, "What does \"janvier\" mean?", "January", "July", "June", "December"));
        q.add(mcq(t, "How do you say 'December' in French?", "décembre", "novembre", "janvier", "mars"));
        q.add(mcq(t, "What does \"mars\" mean?", "March", "May", "April", "June"));

        // Seasons
        q.add(mcq(t, "What does \"l'hiver\" mean?", "winter", "summer", "autumn", "spring"));
        q.add(mcq(t, "How do you say 'summer' in French?", "l'été", "le printemps", "l'hiver", "l'automne"));

        // Transport
        q.add(mcq(t, "What does \"le train\" mean?", "train", "bus", "plane", "bicycle"));
        q.add(mcq(t, "What does \"le vélo\" mean?", "bicycle", "car", "boat", "motorbike"));
        q.add(mcq(t, "What does \"la voiture\" mean?", "car", "bus", "bicycle", "boat"));

        // Places in town
        q.add(mcq(t, "What does \"la boulangerie\" mean?", "bakery", "butcher's shop", "library", "market"));
        q.add(mcq(t, "How do you say 'the library' in French?", "la bibliothèque", "la librairie", "la gare", "le marché"));
        q.add(mcq(t, "What does \"la librairie\" mean?", "bookshop", "library", "bakery", "bank"));
        q.add(mcq(t, "What does \"la gare\" mean?", "train station", "airport", "post office", "market"));
        q.add(mcq(t, "What does \"la pharmacie\" mean?", "pharmacy", "hospital", "bank", "bakery"));
        q.add(mcq(t, "What does \"un magasin\" mean?", "shop", "magazine", "museum", "office"));

        // Clothes
        q.add(mcq(t, "What does \"le chapeau\" mean?", "hat", "shoe", "scarf", "shirt"));
        q.add(mcq(t, "How do you say 'shoes' in French?", "les chaussures", "les chaussettes", "les lunettes", "les gants"));
        q.add(mcq(t, "What does \"la chemise\" mean?", "shirt", "skirt", "dress", "jacket"));

        // Animals and greetings
        q.add(mcq(t, "How do you say 'the cat' in French?", "le chat", "le chien", "la souris", "l'oiseau"));
        q.add(mcq(t, "What does \"au revoir\" mean?", "goodbye", "hello", "thank you", "please"));
        q.add(mcq(t, "What does \"merci\" mean?", "thank you", "please", "sorry", "hello"));
        q.add(mcq(t, "What does \"Comment ça va?\" mean?", "How are you?", "What is your name?", "Where are you?", "How old are you?"));

        // Verbs: avoir, aller, faire, être, vouloir, pouvoir
        q.add(mcq(t, "Complétez: Tu ___ au cinéma ce soir? (aller)", "vas", "vais", "va", "allons"));
        q.add(mcq(t, "Complétez: Nous ___ faim. (avoir)", "avons", "ai", "avez", "ont"));
        q.add(mcq(t, "Complétez: Mon frère et moi ___ un chien. (avoir)", "avons", "ai", "avez", "ont"));
        q.add(mcq(t, "Complétez: Mes parents ___ une maison. (avoir)", "ont", "a", "avons", "avez"));
        q.add(mcq(t, "Complétez: Vous ___ un chien? (avoir)", "avez", "ont", "avons", "as"));
        q.add(mcq(t, "Complétez: Nous ___ du sport. (faire)", "faisons", "faites", "font", "fais"));
        q.add(mcq(t, "Complétez: Il ___ beau aujourd'hui. (faire)", "fait", "fais", "faites", "font"));
        q.add(mcq(t, "Complétez: Je ___ français. (être)", "suis", "es", "est", "sommes"));
        q.add(mcq(t, "Complétez: Tu ___ très gentil. (être)", "es", "est", "suis", "sommes"));
        q.add(mcq(t, "Complétez: Elles ___ des amies. (être)", "sont", "es", "êtes", "sommes"));
        q.add(mcq(t, "Complétez: Tu ___ venir demain? (vouloir)", "veux", "veut", "voulons", "voulez"));
        q.add(mcq(t, "Complétez: Elle ___ nager. (pouvoir)", "peut", "peux", "pouvons", "peuvent"));

        // Regular -er, -ir and -re verbs (present tense)
        q.add(mcq(t, "Complétez: Nous ___ le français. (parler)", "parlons", "parle", "parlent", "parlez"));
        q.add(mcq(t, "Complétez: Ils ___ à la maison. (rester)", "restent", "reste", "restons", "restez"));
        q.add(mcq(t, "Complétez: Je ___ au tennis. (jouer)", "joue", "joues", "jouons", "jouent"));
        q.add(mcq(t, "Complétez: Vous ___ bien. (travailler)", "travaillez", "travaille", "travaillent", "travaillons"));
        q.add(mcq(t, "Complétez: Je ___ mes devoirs. (finir)", "finis", "finit", "finissons", "finissez"));
        q.add(mcq(t, "Complétez: Ils ___ un film. (choisir)", "choisissent", "choisit", "choisis", "choisissez"));
        q.add(mcq(t, "Complétez: Il ___ sa voiture. (vendre)", "vend", "vends", "vendons", "vendent"));
        q.add(mcq(t, "Complétez: Nous ___ le bus. (attendre)", "attendons", "attends", "attendez", "attendent"));

        // Passé composé
        q.add(mcq(t, "Complétez: Hier, j'___ une pizza. (manger, passé composé)", "ai mangé", "suis mangé", "a mangé", "ont mangé"));
        q.add(mcq(t, "Complétez: Hier, nous ___ le film. (voir, passé composé)", "avons vu", "sommes vus", "avons voir", "ont vu"));
        q.add(mcq(t, "Complétez: Hier, Julie ___ un livre. (lire, passé composé)", "a lu", "est lu", "a lire", "a lit"));
        q.add(mcq(t, "Complétez: Hier, Sophie ___ au marché. (aller, passé composé)", "est allée", "est allé", "a allé", "sont allées"));
        q.add(mcq(t, "Complétez: Hier, Marc ___ à l'école. (aller, passé composé)", "est allé", "est allée", "a été", "a allé"));
        q.add(mcq(t, "Complétez: Hier, Chloé ___ à la maison. (rentrer, passé composé)", "est rentrée", "est rentré", "a rentré", "est rentrés"));
        q.add(mcq(t, "Complétez: Hier, nous ___ un gâteau. (faire, passé composé)", "avons fait", "sommes faits", "avons faire", "ont fait"));

        // Futur proche
        q.add(mcq(t, "Complétez: Demain, nous ___ manger au restaurant. (aller)", "allons", "allez", "vont", "vais"));
        q.add(mcq(t, "How do you say 'I am going to eat' in French?", "Je vais manger", "Je suis allé manger", "Je vais mangé", "Je mange"));
        q.add(mcq(t, "How do you say 'They are going to sing' in French?", "Ils vont chanter", "Ils chantent", "Ils sont allés chanter", "Ils vont chanté"));

        // Reflexive verbs
        q.add(mcq(t, "What does \"se lever\" mean?", "to get up", "to wash", "to go to bed", "to sit down"));
        q.add(mcq(t, "Complétez: Je ___ à sept heures. (se lever)", "me lève", "te lève", "se lève", "nous levons"));
        q.add(mcq(t, "What does \"se laver\" mean?", "to wash oneself", "to get up", "to go to bed", "to sit down"));

        // Articles, gender and partitives
        q.add(mcq(t, "Complétez: ___ chien est noir.", "Le", "La", "Les", "L'"));
        q.add(mcq(t, "Complétez: ___ école est grande.", "L'", "La", "Le", "Les"));
        q.add(mcq(t, "Complétez: ___ enfants jouent.", "Les", "Le", "La", "Une"));
        q.add(mcq(t, "Complétez: J'ai ___ sœur.", "une", "un", "le", "des"));
        q.add(mcq(t, "Complétez: Je bois ___ lait. (partitive)", "du", "de la", "des", "de l'"));
        q.add(mcq(t, "Complétez: Elle mange ___ salade. (partitive)", "de la", "du", "des", "de l'"));
        q.add(mcq(t, "Complétez: Il boit ___ eau. (partitive)", "de l'", "du", "de la", "des"));

        // Adjective agreement
        q.add(mcq(t, "Complétez: Une voiture est ___. (blanc)", "blanche", "blanc", "blancs", "blanches"));
        q.add(mcq(t, "Complétez: La maison est ___. (petit)", "petite", "petit", "petits", "petites"));
        q.add(mcq(t, "Complétez: La fille est très ___. (beau)", "belle", "beau", "beaux", "bel"));
        q.add(mcq(t, "Complétez: Les garçons sont ___. (grand)", "grands", "grand", "grande", "grandes"));

        // Possessives
        q.add(mcq(t, "Complétez: ___ père est sympa.", "Mon", "Ma", "Mes", "Ton"));
        q.add(mcq(t, "Complétez: ___ mère est gentille.", "Ma", "Mon", "Mes", "Ta"));
        q.add(mcq(t, "Complétez: ___ amis sont très gentils.", "Mes", "Mon", "Ma", "Ses"));

        // Negation
        q.add(mcq(t, "Which is the negative of \"Je parle français\"?", "Je ne parle pas français",
                "Je ne parle français pas", "Je parle ne pas français", "Je pas parle français"));
        q.add(mcq(t, "What does \"Je ne comprends pas\" mean?", "I don't understand", "I understand", "I don't know", "I don't want"));

        // Question words
        q.add(mcq(t, "What does \"où\" mean?", "where", "who", "when", "why"));
        q.add(mcq(t, "What does \"pourquoi\" mean?", "why", "how", "what", "who"));
        q.add(mcq(t, "What does \"combien\" mean?", "how much / how many", "when", "where", "who"));
        q.add(mcq(t, "What does \"quand\" mean?", "when", "where", "why", "how"));

        // Prepositions
        q.add(mcq(t, "What does \"sur\" mean?", "on", "under", "in", "behind"));
        q.add(mcq(t, "What does \"sous\" mean?", "under", "on", "in", "behind"));
        q.add(mcq(t, "Complétez: Je vais ___ cinéma. (à)", "au", "à la", "à l'", "du"));
        q.add(mcq(t, "Complétez: Je vais ___ école. (à)", "à l'", "au", "à la", "de l'"));

        // French-speaking world
        q.add(mcq(t, "What is the capital of France?", "Paris", "Lyon", "Marseille", "Nice"));
        q.add(mcq(t, "What is the official language of Québec?", "French", "English", "Spanish", "Italian"));

        return q;
    }
}
