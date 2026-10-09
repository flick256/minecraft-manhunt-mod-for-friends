package io.github.flick256.manhunt.core.quiz;

import java.util.ArrayList;
import java.util.List;

/** Easy Biology questions aligned to VCE Biology Units 3 and 4 (VCAA study design). */
public final class BiologyBank {
    private BiologyBank() {}

    public static List<Question> questions() {
        List<Question> q = new ArrayList<>();

        // Unit 3 AOS1: nucleic acids and proteins
        q.add(Question.mcq(Topic.BIOLOGY, "What does the abbreviation DNA stand for?",
                "Deoxyribonucleic acid", "Dinitrogen nucleic acid", "Deoxyribose nitrogen acid", "Double nucleic acid"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which base pairs with adenine in a DNA molecule?",
                "Thymine", "Guanine", "Cytosine", "Uracil"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which sugar forms part of the backbone of a DNA strand?",
                "Deoxyribose", "Glucose", "Fructose", "Sucrose"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which base is found in RNA but not in DNA?",
                "Uracil", "Thymine", "Adenine", "Guanine"));
        q.add(Question.mcq(Topic.BIOLOGY, "What is the monomer (building block) of DNA?",
                "Nucleotide", "Amino acid", "Monosaccharide", "Fatty acid"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which molecule carries the genetic code from DNA to the ribosome?",
                "Messenger RNA (mRNA)", "Transfer RNA (tRNA)", "Ribosomal RNA (rRNA)", "Plasmid"));
        q.add(Question.mcq(Topic.BIOLOGY, "Where does transcription take place in a eukaryotic cell?",
                "Nucleus", "Ribosome", "Golgi apparatus", "Cell membrane"));
        q.add(Question.mcq(Topic.BIOLOGY, "Where does translation take place?",
                "Ribosome", "Nucleus", "Lysosome", "Cell membrane"));
        q.add(Question.mcq(Topic.BIOLOGY, "How many bases make up one codon?",
                "3", "2", "4", "20"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which codon is the start signal for translation?",
                "AUG", "UAA", "UAG", "UGA"));
        q.add(Question.mcq(Topic.BIOLOGY, "What is the monomer (building block) of a protein?",
                "Amino acid", "Nucleotide", "Glucose", "Fatty acid"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which level of protein structure is the sequence of amino acids?",
                "Primary structure", "Secondary structure", "Tertiary structure", "Quaternary structure"));
        q.add(Question.mcq(Topic.BIOLOGY, "What is a gene?",
                "A DNA section coding for protein or RNA", "A protein that cuts DNA", "A type of ribosome",
                "A sugar found in the nucleus"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which of these organisms is a prokaryote?",
                "A bacterium", "A yeast cell", "A human cell", "An oak tree"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which structure is found in eukaryotic cells but not in prokaryotic cells?",
                "A membrane-bound nucleus", "Ribosomes", "A cell membrane", "DNA"));

        // Unit 3 AOS2: enzymes
        q.add(Question.mcq(Topic.BIOLOGY, "Which part of an enzyme does its substrate bind to?",
                "Active site", "Peptide bond", "Cell membrane", "Ribosome"));
        q.add(Question.mcq(Topic.BIOLOGY, "What does an enzyme do to the activation energy of a reaction?",
                "Lowers it", "Raises it", "Doubles it", "Has no effect on it"));
        q.add(Question.mcq(Topic.BIOLOGY, "What usually happens to an enzyme when it is denatured?",
                "Its shape changes and it stops working", "It becomes faster and more active",
                "It is used up by the reaction", "It doubles in size"));
        q.add(Question.mcq(Topic.BIOLOGY, "What is the optimum temperature for most human enzymes?",
                "About 37 degrees C", "About 5 degrees C", "About 80 degrees C", "About 100 degrees C"));
        q.add(Question.mcq(Topic.BIOLOGY, "What is a cofactor?",
                "A non-protein helper for some enzymes", "A type of substrate", "The product of the reaction",
                "A molecule that blocks the enzyme"));
        q.add(Question.mcq(Topic.BIOLOGY, "What is the name for the molecule an enzyme acts on?",
                "Substrate", "Product", "Inhibitor", "Cofactor"));

        // Unit 3 AOS2: photosynthesis
        q.add(Question.mcq(Topic.BIOLOGY, "Where do the light-dependent reactions of photosynthesis take place?",
                "Thylakoid membranes", "Stroma", "Cristae", "Nucleus"));
        q.add(Question.mcq(Topic.BIOLOGY, "Where does the Calvin cycle take place?",
                "Stroma", "Thylakoid membranes", "Cristae", "Nucleus"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which gas is released as a by-product of photosynthesis?",
                "Oxygen", "Carbon dioxide", "Nitrogen", "Methane"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which organelle is the site of photosynthesis?",
                "Chloroplast", "Mitochondrion", "Ribosome", "Vacuole"));

        // Unit 3 AOS2: cellular respiration
        q.add(Question.mcq(Topic.BIOLOGY, "Which organelle is the main site of aerobic cellular respiration?",
                "Mitochondrion", "Chloroplast", "Ribosome", "Vacuole"));
        q.add(Question.mcq(Topic.BIOLOGY, "Where does glycolysis take place in a cell?",
                "Cytoplasm", "Mitochondrial matrix", "Thylakoid membrane", "Nucleus"));
        q.add(Question.mcq(Topic.BIOLOGY, "Where does the Krebs cycle take place?",
                "Mitochondrial matrix", "Cytoplasm", "Thylakoid membrane", "Nucleus"));
        q.add(Question.mcq(Topic.BIOLOGY, "Where is the electron transport chain found in a mitochondrion?",
                "Inner membrane", "Outer membrane", "Matrix", "Cytoplasm"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which product builds up in human muscles during intense anaerobic exercise?",
                "Lactic acid", "Ethanol", "Cholesterol", "Haemoglobin"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which stage of aerobic respiration produces the most ATP?",
                "Electron transport chain", "Glycolysis", "Krebs cycle", "Fermentation"));

        // Unit 4 AOS1: immunity
        q.add(Question.mcq(Topic.BIOLOGY, "Which type of immunity is present from birth and responds the same way each time?",
                "Innate", "Adaptive", "Learned", "Vaccine-based"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which of these is a physical barrier of innate immunity?",
                "Skin", "Antibodies", "Memory cells", "B cells"));
        q.add(Question.mcq(Topic.BIOLOGY, "What is an antigen?",
                "A molecule the immune system recognises", "A cell that produces antibodies",
                "A white blood cell that engulfs germs", "A type of skin barrier"));
        q.add(Question.mcq(Topic.BIOLOGY, "What do B cells produce?",
                "Antibodies", "Antigens", "Platelets", "Red blood cells"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which immune cells directly destroy body cells infected by a virus?",
                "Cytotoxic T cells", "B cells", "Red blood cells", "Platelets"));
        q.add(Question.mcq(Topic.BIOLOGY, "What do memory cells do?",
                "Give a faster response to a repeat infection", "Carry oxygen around the body",
                "Produce insulin in the pancreas", "Clot blood after a cut"));
        q.add(Question.mcq(Topic.BIOLOGY, "What does a vaccine do?",
                "Trains the immune system safely", "Kills the pathogen directly",
                "Replaces damaged white blood cells", "Cures an infection already present"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which of these pathogens is a virus?",
                "Influenza virus", "Staphylococcus aureus", "Candida albicans", "Giardia"));
        q.add(Question.mcq(Topic.BIOLOGY, "What do helper T cells do?",
                "Activate other immune cells", "Produce red blood cells", "Store fat in the liver",
                "Digest food in the stomach"));

        // Unit 4 AOS2: DNA manipulation
        q.add(Question.mcq(Topic.BIOLOGY, "What is the main purpose of PCR?",
                "To make many copies of a DNA segment", "To cut DNA into fragments",
                "To separate proteins by size", "To edit a gene in a cell"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which technique separates DNA fragments by their size?",
                "Gel electrophoresis", "PCR", "Transcription", "Translation"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which enzyme cuts DNA at specific base sequences?",
                "Restriction enzyme", "DNA ligase", "DNA helicase", "RNA polymerase"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which enzyme joins DNA fragments together?",
                "DNA ligase", "Restriction enzyme", "DNA helicase", "RNA polymerase"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which enzyme builds new DNA strands during PCR?",
                "DNA polymerase", "DNA ligase", "Restriction enzyme", "DNA helicase"));
        q.add(Question.mcq(Topic.BIOLOGY, "What does the guide RNA do in CRISPR-Cas9?",
                "Directs Cas9 to a target DNA sequence", "Carries amino acids to ribosomes",
                "Joins DNA fragments together", "Copies the whole genome in PCR"));
        q.add(Question.mcq(Topic.BIOLOGY, "What does the Cas9 enzyme do in CRISPR-Cas9?",
                "Cuts DNA at the target site", "Makes mRNA from a DNA template",
                "Separates DNA fragments by size", "Carries amino acids to ribosomes"));
        q.add(Question.mcq(Topic.BIOLOGY, "What is a genetically modified organism (GMO)?",
                "An organism whose DNA humans altered", "An organism that cannot reproduce",
                "An organism living in extreme heat", "An organism that has lost all its DNA"));
        q.add(Question.mcq(Topic.BIOLOGY, "What is recombinant DNA?",
                "DNA made by joining DNA from two sources", "DNA that has lost its bases",
                "DNA found only in mitochondria", "DNA that cannot be copied"));
        q.add(Question.mcq(Topic.BIOLOGY, "What does DNA sequencing determine?",
                "The order of bases in a DNA molecule", "The size of DNA fragments",
                "How many copies of a gene exist", "The shape of a protein"));

        // Unit 4 AOS3: evolution
        q.add(Question.mcq(Topic.BIOLOGY, "Which scientist wrote On the Origin of Species?",
                "Charles Darwin", "Gregor Mendel", "Louis Pasteur", "Isaac Newton"));
        q.add(Question.mcq(Topic.BIOLOGY, "What is natural selection?",
                "Advantageous traits are passed on more", "Organisms choose the traits they need",
                "All members of a species are identical", "Random changes are always harmful"));
        q.add(Question.mcq(Topic.BIOLOGY, "What is the original source of new alleles?",
                "Mutation", "Predation", "Competition", "Ageing"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which statement describes an adaptation?",
                "A heritable trait that aids survival", "A trait caused only by diet",
                "A mutation that is always harmful", "The age of an organism"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which pair of structures are homologous?",
                "Human arm and bat wing", "Bird wing and insect wing", "Octopus eye and human eye",
                "Shark fin and dolphin fin"));
        q.add(Question.mcq(Topic.BIOLOGY, "What does a phylogenetic tree show?",
                "Evolutionary relationships of organisms", "Growth rate of a plant",
                "Population size over time", "Food chains in a habitat"));
        q.add(Question.mcq(Topic.BIOLOGY, "What is needed for speciation to occur?",
                "Reproductive isolation of populations", "Identical allele frequencies everywhere",
                "A constant environment with no change", "Organisms that never mutate"));
        q.add(Question.mcq(Topic.BIOLOGY, "What is a gene pool?",
                "All the alleles in a population", "All the genes in one organism",
                "A group of closely related species", "The genes of a single cell"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which process produces genetically varied gametes?",
                "Meiosis", "Mitosis", "Binary fission", "Osmosis"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which of these is an example of natural selection?",
                "Resistant bacteria surviving antibiotics", "Muscles growing from weight training",
                "A plant bending toward light", "A cat learning to open a door"));
        q.add(Question.mcq(Topic.BIOLOGY, "Which evidence shows life forms from the past?",
                "Fossils", "Vaccination records", "Blood pressure readings", "Weather records"));

        return q;
    }
}
