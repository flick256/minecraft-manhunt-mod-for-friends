package io.github.flick256.manhunt.core.quiz;

import java.util.ArrayList;
import java.util.List;

/** Easy Chemistry questions aligned to the VCE Chemistry Units 3 and 4 study design (VCAA). */
public final class ChemistryBank {
    private ChemistryBank() {}

    public static List<Question> questions() {
        List<Question> q = new ArrayList<>();

        // Unit 3: fuels and energy
        q.add(Question.mcq(Topic.CHEMISTRY, "Fossil fuels such as coal, oil and natural gas formed from what?",
                "Remains of ancient plants and animals", "Solidified volcanic lava", "Crystallised sea salt", "Compressed sand grains"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Which of these is a biofuel?",
                "Ethanol made by fermenting plant sugars", "Diesel refined from crude oil", "Natural gas from wells", "Coal mined underground"));
        q.add(Question.mcq(Topic.CHEMISTRY, "What must be present, along with a fuel, for combustion to occur?",
                "Oxygen", "Helium", "Argon", "Neon"));
        q.add(Question.mcq(Topic.CHEMISTRY, "What are the main products of complete combustion of a hydrocarbon?",
                "Carbon dioxide and water", "Carbon monoxide and hydrogen", "Oxygen and nitrogen", "Carbon and hydrogen gas"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Which term describes a reaction that releases heat to its surroundings?",
                "Exothermic", "Endothermic", "Neutral", "Catalytic"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Which process absorbs energy from its surroundings?",
                "Photosynthesis", "Burning methane", "Neutralising an acid with a base", "Burning magnesium ribbon"));
        q.add(Question.mcq(Topic.CHEMISTRY, "What fuel property measures energy released per gram or per litre?",
                "Energy density", "Melting point", "Solubility in water", "Colour"));
        q.add(Question.mcq(Topic.CHEMISTRY, "What is the main product of a hydrogen fuel cell that uses hydrogen and oxygen?",
                "Water", "Carbon dioxide", "Carbon monoxide", "Sulfur dioxide"));
        q.add(Question.mcq(Topic.CHEMISTRY, "What does a calorimeter measure?",
                "Heat energy transferred", "Electric current flow", "Gas pressure", "Solution pH"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Which fuel is classed as renewable?",
                "Biodiesel made from plant oils", "Crude oil", "Coal", "Natural gas"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Which gas is the main component of natural gas?",
                "Methane", "Propane", "Butane", "Ethene"));

        // Unit 3: galvanic cells and electrolysis
        q.add(Question.mcq(Topic.CHEMISTRY, "In a galvanic cell, where does oxidation occur?",
                "At the anode", "At the cathode", "In the salt bridge", "In the external wire"));
        q.add(Question.mcq(Topic.CHEMISTRY, "In a galvanic cell, electrons move through the external wire from where to where?",
                "Anode to cathode", "Cathode to anode", "Salt bridge to cathode", "Cathode to salt bridge"));
        q.add(Question.mcq(Topic.CHEMISTRY, "What is the main purpose of the salt bridge in a galvanic cell?",
                "To let ions flow and close the circuit", "To carry electrons between electrodes",
                "To supply oxygen to the cathode", "To heat the electrolyte"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Which metal is most easily oxidised: copper, magnesium, silver or gold?",
                "Magnesium", "Copper", "Silver", "Gold"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Which halogen is the strongest oxidising agent?",
                "Fluorine", "Chlorine", "Bromine", "Iodine"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Which type of cell can be recharged by reversing its reaction?",
                "Secondary cell", "Primary cell", "Single-use cell", "Disposable cell"));
        q.add(Question.mcq(Topic.CHEMISTRY, "What must an electrolyte contain so it can conduct electricity?",
                "Mobile ions", "Only neutral molecules", "Only metal atoms", "Only free electrons"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Electrolysis uses electrical energy to drive what kind of reaction?",
                "A non-spontaneous reaction", "A spontaneous reaction", "Neutralisation", "Combustion"));
        q.add(Question.mcq(Topic.CHEMISTRY, "In electrolysis of molten sodium chloride, what forms at the cathode?",
                "Sodium", "Chlorine", "Hydrogen", "Oxygen"));
        q.add(Question.mcq(Topic.CHEMISTRY, "What gas forms at the anode when molten sodium chloride is electrolysed?",
                "Chlorine", "Hydrogen", "Oxygen", "Nitrogen"));
        q.add(Question.mcq(Topic.CHEMISTRY, "In electrolysis of water with inert electrodes, what gas forms at the cathode?",
                "Hydrogen", "Oxygen", "Chlorine", "Carbon dioxide"));
        q.add(Question.mcq(Topic.CHEMISTRY, "In electroplating, what happens if a larger current flows for a longer time?",
                "More metal is deposited", "Less metal is deposited", "The same mass is deposited", "No metal is deposited"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Which electrode material is inert and commonly used in electrolysis?",
                "Graphite", "Zinc", "Magnesium", "Sodium"));

        // Unit 3: rates, equilibrium and acids/bases
        q.add(Question.mcq(Topic.CHEMISTRY, "Increasing temperature speeds up a reaction mainly because particles have what?",
                "More kinetic energy", "Less kinetic energy", "Greater mass", "Fewer collisions"));
        q.add(Question.mcq(Topic.CHEMISTRY, "How does a catalyst increase the rate of a reaction?",
                "By lowering the activation energy", "By raising the activation energy",
                "By using up more reactant", "By increasing the temperature"));
        q.add(Question.mcq(Topic.CHEMISTRY, "What is activation energy?",
                "Minimum energy needed to react", "Energy released by a reaction",
                "Energy stored in the products", "Energy of the catalyst"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Is a catalyst used up in the reaction it speeds up?",
                "No, it is not used up", "Yes, it is fully consumed", "Yes, it becomes a product", "Yes, it doubles in mass"));
        q.add(Question.mcq(Topic.CHEMISTRY, "In a dynamic equilibrium, what is true?",
                "Forward and reverse rates are equal", "All reactions have stopped",
                "Only the forward reaction occurs", "Concentrations are all zero"));
        q.add(Question.mcq(Topic.CHEMISTRY, "For an exothermic reaction at equilibrium, raising the temperature shifts the position towards",
                "The reactants", "The products", "No shift occurs", "The catalyst"));
        q.add(Question.mcq(Topic.CHEMISTRY, "If the gas amounts differ either side, higher pressure shifts equilibrium to the side with:",
                "Fewer gas molecules", "More gas molecules", "Liquid molecules only", "No gas molecules"));
        q.add(Question.mcq(Topic.CHEMISTRY, "The value of the equilibrium constant K changes only if what changes?",
                "Temperature", "Catalyst", "Pressure", "Concentration"));
        q.add(Question.mcq(Topic.CHEMISTRY, "A large equilibrium constant K means what?",
                "Products are favoured at equilibrium", "Reactants are favoured",
                "Reaction is very fast", "No reaction is occurring"));
        q.add(Question.mcq(Topic.CHEMISTRY, "A solution with a pH of 3 is what?",
                "Acidic", "Neutral", "Basic", "Saturated"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Which of these is a strong acid?",
                "Hydrochloric acid", "Ethanoic acid", "Carbonic acid", "Citric acid"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Which statement describes a weak acid?",
                "It only partly ionises in water", "It fully ionises in water",
                "It has a pH of 0", "It is a strong base"));
        q.add(Question.mcq(Topic.CHEMISTRY, "An acid reacting with a base (neutralisation) produces what?",
                "A salt and water", "A salt and hydrogen", "Oxygen and water", "Carbon dioxide and water"));

        // Unit 4: organic chemistry
        q.add(Question.mcq(Topic.CHEMISTRY, "What is the general formula of alkanes?",
                "CnH2n+2", "CnH2n", "CnH2n-2", "CnH3n"));
        q.add(Question.mcq(Topic.CHEMISTRY, "What is the general formula of alkenes?",
                "CnH2n", "CnH2n+2", "CnH2n-2", "CnH3n+2"));
        q.add(Question.mcq(Topic.CHEMISTRY, "What is the name of the alkyne with two carbon atoms?",
                "Ethyne", "Ethene", "Ethane", "Propyne"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Which alkane has six carbon atoms in each molecule?",
                "Hexane", "Pentane", "Heptane", "Butane"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Which functional group is present in alcohols?",
                "Hydroxyl group, -OH", "Carboxyl group, -COOH", "Amine group, -NH2", "Aldehyde group, -CHO"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Which functional group defines a carboxylic acid?",
                "-COOH", "-CHO", "-NH2", "-C=C-"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Esterification of an alcohol with a carboxylic acid produces what?",
                "An ester and water", "An alkene and water", "Two alcohols", "A polymer only"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Amino acids contain which two functional groups?",
                "Amine and carboxylic acid groups", "Alcohol and ketone groups",
                "Ester and alkene groups", "Aldehyde and ether groups"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Which functional group is -CHO?",
                "Aldehyde", "Ketone", "Alcohol", "Ester"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Which functional group has a C=O bonded to two carbon atoms?",
                "Ketone", "Aldehyde", "Ester", "Alcohol"));
        q.add(Question.mcq(Topic.CHEMISTRY, "What are compounds with the same molecular formula but different structures called?",
                "Isomers", "Homologues", "Allotropes", "Polymers"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Which polymer forms from addition polymerisation of ethene?",
                "Polyethene", "Nylon", "Polyester", "Starch"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Addition polymerisation needs monomers that contain what?",
                "A carbon-carbon double bond", "A carboxylic acid group", "Only single bonds", "An ester linkage"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Proteins are chains of amino acids linked by which bonds?",
                "Peptide bonds", "Ester bonds", "Glycosidic bonds", "Ionic bonds"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Fats and oils are esters of which two substances?",
                "Glycerol and fatty acids", "Glucose and fructose", "Ethanol and ethanoic acid", "Glucose and amino acids"));
        q.add(Question.mcq(Topic.CHEMISTRY, "Glucose is an example of which type of carbohydrate?",
                "Monosaccharide", "Disaccharide", "Polysaccharide", "Triglyceride"));

        // Analysis techniques and the mole concept
        q.add(Question.mcq(Topic.CHEMISTRY, "Which peak in a mass spectrum gives the relative molecular mass?",
                "Molecular ion peak", "Base peak", "Solvent peak", "Baseline"));
        q.add(Question.mcq(Topic.CHEMISTRY, "A broad absorption near 3300 cm^-1 in an IR spectrum most likely indicates which bond?",
                "O-H", "C=O", "C-Cl", "C=C"));
        q.add(Question.mcq(Topic.CHEMISTRY, "A strong absorption near 1700 cm^-1 in an IR spectrum indicates which bond?",
                "C=O", "C-H", "O-H", "C-O"));
        q.add(Question.mcq(Topic.CHEMISTRY, "What does the number of signals in a proton NMR spectrum tell you?",
                "Different hydrogen environments", "Total number of carbon atoms",
                "Molecular mass", "Number of oxygen atoms"));
        q.add(Question.mcq(Topic.CHEMISTRY, "In thin-layer chromatography, a larger Rf value means a spot moved what?",
                "Further up the plate", "Less far up the plate", "Not at all", "Backwards down the plate"));
        q.add(Question.mcq(Topic.CHEMISTRY, "In gas chromatography, what is retention time?",
                "Time taken to pass through the column", "Time taken to heat the sample",
                "Time taken to form the spectrum", "Time taken to dissolve the sample"));
        q.add(Question.mcq(Topic.CHEMISTRY, "At the equivalence point of an acid-base titration, what is true?",
                "Acid and base reacted in exact ratio", "Acid is in large excess",
                "Indicator has just been added", "Solution is always pure water"));
        q.add(Question.mcq(Topic.CHEMISTRY, "What is the purpose of an indicator in a titration?",
                "To show the end point by colour change", "To speed up the reaction",
                "To neutralise the acid", "To measure the volume added"));
        q.add(Question.mcq(Topic.CHEMISTRY, "What is Avogadro's number, the number of particles in one mole?",
                "6.02 x 10^23", "6.02 x 10^21", "6.02 x 10^26", "1.00 x 10^23"));
        q.add(Question.mcq(Topic.CHEMISTRY, "What is the molar mass of CO2 in g/mol (C = 12, O = 16)?",
                "44", "12", "28", "32"));
        q.add(Question.mcq(Topic.CHEMISTRY, "How many moles are in 36 g of water (molar mass 18 g/mol)?",
                "2", "18", "0.5", "36"));

        return q;
    }
}
