package io.github.flick256.manhunt.core.quiz;

import static io.github.flick256.manhunt.core.quiz.Topic.HISTORY;

import java.util.ArrayList;
import java.util.List;

/** Easy History questions aligned to VCE History (Revolutions and Australian history), Units 3 and 4. */
public final class HistoryBank {
    private HistoryBank() {}

    public static List<Question> questions() {
        List<Question> q = new ArrayList<>();

        // French Revolution
        q.add(Question.mcq(HISTORY, "Which assembly did King Louis XVI convene in May 1789, helping to trigger the French Revolution?",
                "The Estates-General", "The Reichstag", "The Diet of Worms", "The Duma"));
        q.add(Question.mcq(HISTORY, "Which estate in pre-revolutionary France was made up of the common people?",
                "The Third Estate", "The First Estate", "The Second Estate", "The Fourth Estate"));
        q.add(Question.mcq(HISTORY, "What was the Tennis Court Oath (June 1789)?",
                "Vow not to disband until a constitution", "Obey the king without question",
                "Abolish the monarchy at once", "Raise taxes on the clergy"));
        q.add(Question.mcq(HISTORY, "On what date was the Bastille stormed in 1789? It is now France's national day.",
                "14 July", "4 July", "14 August", "25 December"));
        q.add(Question.mcq(HISTORY, "Which 1789 French document declared the rights of individuals and citizens?",
                "Declaration of the Rights of Man", "Declaration of Independence", "Treaty of Versailles", "Magna Carta"));
        q.add(Question.mcq(HISTORY, "Who was the king of France at the start of the French Revolution?",
                "Louis XVI", "Louis XIV", "Napoleon III", "Henry IV"));
        q.add(Question.mcq(HISTORY, "Marie Antoinette was the queen consort of which French king?",
                "Louis XVI", "Louis XIV", "Charles X", "Henry IV"));
        q.add(Question.mcq(HISTORY, "Which radical political club was central to the Reign of Terror?",
                "The Jacobins", "The Bonapartists", "The Hanoverians", "The Whigs"));
        q.add(Question.mcq(HISTORY, "Which Jacobin leader became the most powerful figure of the Reign of Terror?",
                "Maximilien Robespierre", "Napoleon Bonaparte", "Marquis de Lafayette", "Louis XVI"));
        q.add(Question.mcq(HISTORY, "Which body ran the Reign of Terror in France in 1793-94?",
                "Committee of Public Safety", "House of Lords", "Reichstag", "Congress of Vienna"));
        q.add(Question.mcq(HISTORY, "Which machine became the symbol of executions during the French Revolution?",
                "The guillotine", "The gallows", "The firing squad", "The iron maiden"));
        q.add(Question.mcq(HISTORY, "In what year did Napoleon Bonaparte seize power in France in a coup?",
                "1799", "1789", "1812", "1815"));
        q.add(Question.mcq(HISTORY, "In what year did Napoleon crown himself Emperor of the French?",
                "1804", "1799", "1815", "1821"));
        q.add(Question.mcq(HISTORY, "Before 1789, what did the term 'Ancien Régime' describe in France?",
                "Absolute monarchy and privilege", "A republic run by elected deputies",
                "Napoleon's imperial government", "Rule by the Roman Senate"));
        q.add(Question.mcq(HISTORY, "In what year was King Louis XVI executed by guillotine?",
                "1793", "1789", "1799", "1815"));

        // Russian Revolution
        q.add(Question.mcq(HISTORY, "Who was the last Tsar of Russia?",
                "Nicholas II", "Alexander II", "Ivan the Terrible", "Peter the Great"));
        q.add(Question.mcq(HISTORY, "Which mystic had great influence over Tsarina Alexandra before 1917?",
                "Rasputin", "Trotsky", "Lenin", "Stalin"));
        q.add(Question.mcq(HISTORY, "Which 1917 revolution forced Tsar Nicholas II to abdicate?",
                "The February Revolution", "Revolution of 1905", "Decembrist Revolt", "Velvet Revolution"));
        q.add(Question.mcq(HISTORY, "Which body took over governing Russia after the Tsar abdicated in 1917?",
                "The Provisional Government", "The Politburo", "The Comintern", "The Supreme Soviet"));
        q.add(Question.mcq(HISTORY, "Who led the Bolsheviks to power in the October 1917 revolution?",
                "Vladimir Lenin", "Alexander Kerensky", "Nicholas II", "Rasputin"));
        q.add(Question.mcq(HISTORY, "Lenin's April Theses (1917) demanded that all power be handed to which bodies?",
                "The Soviets", "The Tsar", "The Church", "The Provisional Government"));
        q.add(Question.mcq(HISTORY, "What is the Bolshevik seizure of power in late 1917 called?",
                "October Revolution", "February Revolution", "Revolution of 1905", "Velvet Revolution"));
        q.add(Question.mcq(HISTORY, "Leon Trotsky organised which army during the Russian Civil War?",
                "The Red Army", "The White Army", "Imperial Russian Guard", "Ottoman Army"));
        q.add(Question.mcq(HISTORY, "In the Russian Civil War, which side was led by the Bolsheviks?",
                "The Reds", "The Whites", "The Blues", "The Blacks"));
        q.add(Question.mcq(HISTORY, "Which Soviet policy during the Civil War seized grain from peasants?",
                "War Communism", "Marshall Plan", "Truman Doctrine", "Dawes Plan"));
        q.add(Question.mcq(HISTORY, "In what year did Lenin introduce the New Economic Policy (NEP)?",
                "1921", "1917", "1905", "1939"));
        q.add(Question.mcq(HISTORY, "Who became the dominant Soviet leader after Lenin died in 1924?",
                "Joseph Stalin", "Alexander Kerensky", "Nicholas II", "Mikhail Gorbachev"));

        // Chinese Revolution
        q.add(Question.mcq(HISTORY, "In what year did the Xinhai Revolution begin, overthrowing the Qing dynasty?",
                "1911", "1905", "1927", "1949"));
        q.add(Question.mcq(HISTORY, "Who is regarded as the founding father of the Republic of China?",
                "Sun Yat-sen", "Mao Zedong", "Chiang Kai-shek", "Deng Xiaoping"));
        q.add(Question.mcq(HISTORY, "Who was China's last emperor?",
                "Puyi", "Kangxi", "Guangxu", "Qianlong"));
        q.add(Question.mcq(HISTORY, "Chiang Kai-shek led which Chinese nationalist party?",
                "The Kuomintang", "The Chinese Communist Party", "The Boxer Society", "The Taiping Movement"));
        q.add(Question.mcq(HISTORY, "Which leader fled to Taiwan in 1949 after losing the civil war to the communists?",
                "Chiang Kai-shek", "Mao Zedong", "Deng Xiaoping", "Sun Yat-sen"));
        q.add(Question.mcq(HISTORY, "Mao Zedong led which party to power in mainland China in 1949?",
                "The Chinese Communist Party", "The Kuomintang", "The Qing court", "The Boxer movement"));
        q.add(Question.mcq(HISTORY, "The Long March (1934-35) was an epic retreat undertaken by which forces?",
                "Chinese Communist Red Army", "Kuomintang army", "Japanese Imperial Army", "British colonial forces"));
        q.add(Question.mcq(HISTORY, "In what year was the People's Republic of China proclaimed?",
                "1949", "1911", "1945", "1956"));
        q.add(Question.mcq(HISTORY, "Which late-1950s campaign aimed to industrialise China quickly and caused mass famine?",
                "The Great Leap Forward", "The Long March", "The Cultural Revolution", "The Boxer Rebellion"));
        q.add(Question.mcq(HISTORY, "Mao Zedong launched which mass movement in 1966?",
                "The Cultural Revolution", "The Long March", "The Taiping Rebellion", "The Boxer Rebellion"));
        q.add(Question.mcq(HISTORY, "The Red Guards of the 1960s were mainly made up of whom?",
                "Young students", "Retired army generals", "Wealthy landowners", "Foreign diplomats"));

        // American Revolution
        q.add(Question.mcq(HISTORY, "In what year was the Boston Tea Party held?",
                "1773", "1765", "1776", "1783"));
        q.add(Question.mcq(HISTORY, "The slogan 'No taxation without representation' protested against what?",
                "Taxes with no colonial representation", "Limits on western land purchase",
                "Ban on colonial churches", "Forced service in British army"));
        q.add(Question.mcq(HISTORY, "In what year was the Declaration of Independence adopted?",
                "1776", "1773", "1783", "1789"));
        q.add(Question.mcq(HISTORY, "Who was the principal author of the Declaration of Independence?",
                "Thomas Jefferson", "Patrick Henry", "Paul Revere", "King George III"));
        q.add(Question.mcq(HISTORY, "Who commanded the Continental Army during the American Revolution?",
                "George Washington", "Thomas Jefferson", "Benjamin Franklin", "John Hancock"));
        q.add(Question.mcq(HISTORY, "In what year was the Treaty of Paris signed, ending the American Revolutionary War?",
                "1783", "1776", "1781", "1789"));

        // Australian history
        q.add(Question.mcq(HISTORY, "In what year did the First Fleet arrive at Sydney Cove?",
                "1788", "1770", "1851", "1901"));
        q.add(Question.mcq(HISTORY, "Which British naval officer led the First Fleet to Sydney Cove?",
                "Arthur Phillip", "James Cook", "Matthew Flinders", "Lachlan Macquarie"));
        q.add(Question.mcq(HISTORY, "The Eureka Stockade (1854) took place in which Australian colony?",
                "Victoria", "New South Wales", "Tasmania", "Queensland"));
        q.add(Question.mcq(HISTORY, "In what year did the six Australian colonies federate as the Commonwealth?",
                "1901", "1788", "1854", "1967"));
        q.add(Question.mcq(HISTORY, "Who was Australia's first Prime Minister?",
                "Edmund Barton", "Robert Menzies", "John Curtin", "Ben Chifley"));
        q.add(Question.mcq(HISTORY, "The 1967 referendum concerned which group of Australians?",
                "Aboriginal and Torres Strait Islanders", "Convict descendants",
                "Pacific Islander workers", "Chinese gold miners"));
        q.add(Question.mcq(HISTORY, "The 1992 Mabo decision rejected which legal doctrine as applied to Australia?",
                "Terra nullius", "Habeas corpus", "Manifest destiny", "Divine right of kings"));
        q.add(Question.mcq(HISTORY, "Under past assimilation policies, which group's children were forcibly removed from families?",
                "Indigenous children", "Children of convicts", "Children of Chinese migrants", "Children of gold miners"));

        return q;
    }
}
