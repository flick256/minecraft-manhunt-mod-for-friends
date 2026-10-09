package io.github.flick256.manhunt.core.quiz;

import java.util.ArrayList;
import java.util.List;

/** Easy Physics questions, aligned to the VCE Physics Units 3 and 4 study design. */
public final class PhysicsBank {
    private PhysicsBank() {}

    public static List<Question> questions() {
        List<Question> q = new ArrayList<>();
        Topic t = Topic.PHYSICS;

        // Unit 3: motion, forces and energy
        q.add(Question.mcq(t, "Near Earth's surface, the gravitational field strength is approximately:",
                "9.8 N/kg", "98 N/kg", "0.98 N/kg", "1.6 N/kg"));
        q.add(Question.mcq(t, "Which equation gives the weight of an object?",
                "W = mg", "W = m/g", "W = mv", "W = g/m"));
        q.add(Question.mcq(t, "Which equation is Newton's second law of motion?",
                "F = ma", "F = mv", "F = m/a", "F = mgh"));
        q.add(Question.mcq(t, "A 2 kg object accelerates at 3 m/s^2. What is the net force on it?",
                "6 N", "1.5 N", "5 N", "0.67 N"));
        q.add(Question.mcq(t, "According to Newton's first law, with no net force an object will:",
                "Keep its velocity unchanged", "Slow down steadily", "Accelerate downward", "Move in a circle"));
        q.add(Question.mcq(t, "When object A pushes on object B, Newton's third law says B:",
                "Pushes back on A, equal and opposite", "Pushes A with a larger force",
                "Does nothing to A", "Absorbs the force"));
        q.add(Question.mcq(t, "Ignoring air resistance, a projectile's horizontal velocity:",
                "Stays constant", "Steadily increases", "Steadily decreases", "Reverses direction"));
        q.add(Question.mcq(t, "Ignoring air resistance, a projectile's vertical acceleration is:",
                "Downward, due to gravity", "Upward, due to gravity", "Zero at all times", "Sideways"));
        q.add(Question.mcq(t, "In uniform circular motion, the net force points:",
                "Towards the centre of the circle", "Away from the centre",
                "Along the direction of motion", "Straight up"));
        q.add(Question.mcq(t, "Kinetic energy is calculated using:",
                "Ek = 1/2 mv^2", "Ek = mgh", "Ek = mv", "Ek = Fd"));
        q.add(Question.mcq(t, "Gravitational potential energy is calculated using:",
                "Ep = mgh", "Ep = 1/2 mv^2", "Ep = ma", "Ep = mv"));
        q.add(Question.mcq(t, "The SI unit of energy is the:",
                "Joule (J)", "Newton (N)", "Watt (W)", "Pascal (Pa)"));
        q.add(Question.mcq(t, "Work done by a force is calculated as:",
                "W = F x d (force x distance moved)", "W = F / d", "W = ma", "W = mv"));
        q.add(Question.mcq(t, "Momentum of an object is calculated as:",
                "p = mv", "p = ma", "p = m/v", "p = 1/2 mv^2"));
        q.add(Question.mcq(t, "In a collision with no external forces, the total momentum:",
                "Stays the same", "Always increases", "Always becomes zero", "Halves"));
        q.add(Question.mcq(t, "The law of conservation of energy says energy:",
                "Cannot be created or destroyed", "Can be created from nothing",
                "Always disappears", "Only exists in moving objects"));
        q.add(Question.mcq(t, "The tendency of an object to resist changes in its motion is called:",
                "Inertia", "Friction", "Buoyancy", "Gravity"));
        q.add(Question.mcq(t, "Acceleration is the rate of change of:",
                "Velocity", "Displacement", "Mass", "Energy"));
        q.add(Question.mcq(t, "Compared with its weight on Earth, an object's weight on the Moon is:",
                "Less (the Moon's gravity is weaker)", "Greater", "Exactly the same", "Zero"));

        // Unit 3: electric and magnetic fields
        q.add(Question.mcq(t, "Two positive charges placed near each other will:",
                "Repel each other", "Attract each other", "Not interact at all", "Spin around each other"));
        q.add(Question.mcq(t, "Electric field lines point:",
                "From positive towards negative charges", "From negative towards positive charges",
                "In circles around the charge", "Only into the charge"));
        q.add(Question.mcq(t, "The electric force between two charges gets stronger when they are:",
                "Closer together", "Further apart", "Both neutral", "Moved sideways only"));
        q.add(Question.mcq(t, "A neutral object has:",
                "Equal numbers of protons and electrons", "More protons than electrons",
                "More electrons than protons", "No protons at all"));
        q.add(Question.mcq(t, "The charge of an electron is:",
                "Negative", "Positive", "Neutral", "Variable"));
        q.add(Question.mcq(t, "Outside a bar magnet, magnetic field lines run from:",
                "North pole to south pole", "South pole to north pole",
                "The centre to the edges only", "One end to the other only in circles"));
        q.add(Question.mcq(t, "Two north magnetic poles placed together will:",
                "Repel each other", "Attract each other", "Stick permanently", "Have no effect"));
        q.add(Question.mcq(t, "A current-carrying wire in a magnetic field feels a force that is:",
                "Perpendicular to current and field", "Parallel to the current",
                "Parallel to the field lines", "Always zero"));
        q.add(Question.mcq(t, "Which rule finds the magnetic field direction around a current-carrying wire?",
                "Right-hand grip rule", "Newton's second law", "Ohm's law", "Lenz's law"));
        q.add(Question.mcq(t, "Magnetic fields are produced by:",
                "Electric currents (moving charges)", "Stationary neutral objects", "Gravity", "Sound waves"));

        // Unit 3: electromagnetic induction and electricity
        q.add(Question.mcq(t, "Electromagnetic induction produces an emf when:",
                "Magnetic flux through a coil changes", "Magnetic flux stays constant",
                "The coil is made of plastic", "No magnetic field is present anywhere"));
        q.add(Question.mcq(t, "Moving a magnet faster into a coil produces:",
                "A larger induced emf", "A smaller induced emf", "No induced emf",
                "The same emf regardless of speed"));
        q.add(Question.mcq(t, "Lenz's law says an induced current always opposes:",
                "The change that produces it", "The resistance of the wire",
                "Gravity acting on the magnet", "The temperature of the coil"));
        q.add(Question.mcq(t, "A step-up transformer has more turns on its:",
                "Secondary coil", "Primary coil", "Iron core", "Neither coil"));
        q.add(Question.mcq(t, "A transformer with more primary turns than secondary turns is a:",
                "Step-down transformer", "Step-up transformer", "DC motor", "Simple resistor"));
        q.add(Question.mcq(t, "Long-distance power lines use high voltage mainly to:",
                "Reduce energy lost as heat", "Increase the current in the wires",
                "Make the wires heavier", "Convert DC into AC"));
        q.add(Question.mcq(t, "Mains electricity in Australia has a frequency of:",
                "50 Hz", "60 Hz", "25 Hz", "240 Hz"));
        q.add(Question.mcq(t, "Which describes alternating current (AC)?",
                "Current reverses direction periodically", "Flows in one direction only",
                "Only comes from batteries", "Has zero frequency"));
        q.add(Question.mcq(t, "A generator converts mechanical energy into:",
                "Electrical energy", "Chemical energy", "Nuclear energy", "Light energy only"));
        q.add(Question.mcq(t, "An electric motor mainly converts electrical energy into:",
                "Kinetic (movement) energy", "Chemical energy", "Nuclear energy", "Potential energy only"));
        q.add(Question.mcq(t, "Electrical power is calculated using:",
                "P = VI", "P = V/I", "P = I^2 + V", "P = R/V"));
        q.add(Question.mcq(t, "Ohm's law is written as:",
                "V = IR", "V = I/R", "V = R/I", "V = IR^2"));
        q.add(Question.mcq(t, "The SI unit of electric current is the:",
                "Ampere (A)", "Volt (V)", "Ohm", "Coulomb (C)"));
        q.add(Question.mcq(t, "The unit of electrical resistance is the:",
                "Ohm", "Ampere (A)", "Volt (V)", "Watt (W)"));
        q.add(Question.mcq(t, "The unit of potential difference (voltage) is the:",
                "Volt (V)", "Ampere (A)", "Ohm", "Joule (J)"));
        q.add(Question.mcq(t, "In a series circuit, the current through each component is:",
                "The same", "Different for each component", "Zero", "Doubled at each resistor"));
        q.add(Question.mcq(t, "In a parallel circuit, the voltage across each branch is:",
                "The same as the supply voltage", "Zero in every branch",
                "Shared equally between branches", "Halved in each branch"));
        q.add(Question.mcq(t, "Two 10 ohm resistors in series have a total resistance of:",
                "20 ohms", "5 ohms", "10 ohms", "100 ohms"));
        q.add(Question.mcq(t, "Two 10 ohm resistors in parallel have a combined resistance of:",
                "5 ohms", "20 ohms", "10 ohms", "2 ohms"));
        q.add(Question.mcq(t, "A 12 V supply is connected across a 4 ohm resistor. The current is:",
                "3 A", "8 A", "48 A", "0.33 A"));
        q.add(Question.mcq(t, "A device draws 2 A at 10 V. Its power is:",
                "20 W", "5 W", "12 W", "200 W"));

        // Unit 4: waves and light
        q.add(Question.mcq(t, "Wave speed is related to frequency and wavelength by:",
                "v = f x wavelength", "v = f / wavelength", "v = f + wavelength", "v = wavelength / f"));
        q.add(Question.mcq(t, "A wave has frequency 5 Hz and wavelength 2 m. Its speed is:",
                "10 m/s", "2.5 m/s", "7 m/s", "0.4 m/s"));
        q.add(Question.mcq(t, "The speed of light in a vacuum is approximately:",
                "3.0 x 10^8 m/s", "3.0 x 10^6 m/s", "3.0 x 10^5 m/s", "3.0 x 10^10 m/s"));
        q.add(Question.mcq(t, "The SI unit of frequency is the:",
                "Hertz (Hz)", "Metre (m)", "Second (s)", "Joule (J)"));
        q.add(Question.mcq(t, "The amplitude of a wave is its:",
                "Maximum displacement from rest", "Number of waves per second",
                "Length of one full wave", "Speed of travel"));
        q.add(Question.mcq(t, "Polarisation of light shows that light is a:",
                "Transverse wave", "Longitudinal wave", "Stream of electrons", "Magnetic field"));
        q.add(Question.mcq(t, "Which result in the double-slit experiment shows light is a wave?",
                "Bright and dark interference bands", "Electrons ejected from metal",
                "Light moving in straight lines", "Photons hitting a detector"));
        q.add(Question.mcq(t, "Constructive interference happens when two waves meet:",
                "Crest to crest (in phase)", "Crest to trough (out of phase)",
                "At right angles", "Only when stationary"));
        q.add(Question.mcq(t, "Diffraction is most noticeable when the gap width is:",
                "About the same size as the wavelength", "Much larger than the wavelength",
                "Exactly zero", "Much larger than the frequency"));

        // Unit 4: light and matter
        q.add(Question.mcq(t, "The photoelectric effect provides evidence that light behaves as:",
                "A stream of particles (photons)", "A longitudinal wave only",
                "A magnetic field only", "A stationary charge"));
        q.add(Question.mcq(t, "In the photoelectric effect, no electrons are emitted when light frequency is:",
                "Below the threshold frequency", "Above the threshold frequency",
                "Any frequency if bright enough", "Always zero"));
        q.add(Question.mcq(t, "Photon energy is calculated using:",
                "E = hf", "E = mc^2", "E = h / f", "E = f / h"));
        q.add(Question.mcq(t, "A particle of light is called a:",
                "Photon", "Neutron", "Proton", "Quark"));
        q.add(Question.mcq(t, "Electron diffraction experiments show that electrons have:",
                "Wave properties", "No mass", "Only particle properties", "A positive charge"));
        q.add(Question.mcq(t, "The idea that light and electrons show both wave and particle behaviour is:",
                "Wave-particle duality", "Charge duality", "Frequency doubling", "Magnetic shielding"));
        q.add(Question.mcq(t, "Light of higher frequency has:",
                "Higher photon energy", "Lower photon energy",
                "Lower speed in a vacuum", "No energy"));

        // Unit 4: special relativity
        q.add(Question.mcq(t, "A key postulate of special relativity is that the speed of light:",
                "Is the same for all observers", "Depends on the observer's speed",
                "Is faster for moving observers", "Is zero in a vacuum"));
        q.add(Question.mcq(t, "Time dilation means a clock moving relative to you runs:",
                "Slower than your clock", "Faster than your clock", "Exactly the same", "Backwards"));
        q.add(Question.mcq(t, "Length contraction makes a fast-moving object appear:",
                "Shorter in its direction of motion", "Longer in its direction of motion",
                "Wider from the side", "Unchanged in every direction"));
        q.add(Question.mcq(t, "In E = mc^2, what does c represent?",
                "The speed of light in a vacuum", "The speed of sound",
                "The charge of an electron", "Half the speed of light"));
        q.add(Question.mcq(t, "Rest mass-energy comes from an object's:",
                "Mass (E = mc^2)", "Speed", "Temperature", "Electric charge"));
        q.add(Question.mcq(t, "Which can travel at exactly the speed of light in a vacuum?",
                "A photon", "A baseball", "A car", "An electron"));

        return q;
    }
}
