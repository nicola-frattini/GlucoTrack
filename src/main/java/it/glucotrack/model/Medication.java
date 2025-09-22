package it.glucotrack.model;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import it.glucotrack.util.LogMedicationDAO;





public class Medication {

    private int id;
    private int patient_id;
    private String name_medication;
    private String dose; // dose as string (e.g., "500mg", "10 unità")
    private Frequency freq; // enum - once a day, twice a day, etc
    private LocalDate start_date;
    private LocalDate end_date;
    private String instructions; // text
    private List<LogMedication> log_medications;

    // ===== Default constructor =====
    public Medication() {
        this.id = -1;
        this.patient_id = -1;
        this.name_medication = "";
        this.dose = "";
        this.freq = Frequency.ONCE_A_DAY;
        this.start_date = LocalDate.now();
        this.end_date = LocalDate.now();
        this.instructions = "";
        this.log_medications = new ArrayList<>();
    }

    // ===== Constructor without ID (for new records) =====
    public Medication(int patient_id, String name_medication, String dose, Frequency freq, LocalDate start_date, LocalDate end_date, String instructions) {
        this.id = -1; // Will be set by database
        this.patient_id = patient_id;
        this.name_medication = name_medication;
        this.dose = dose;
        this.freq = freq;
        this.start_date = start_date;
        this.end_date = end_date;
        this.instructions = instructions;
        this.log_medications = new ArrayList<>();
    }

    // ===== Full constructor =====
    public Medication(int id, int patient_id, String name_medication, String dose, Frequency freq, LocalDate start_date, LocalDate end_date, String instructions) {
        this.id = id;
        this.patient_id = patient_id;
        this.name_medication = name_medication;
        this.dose = dose;
        this.freq = freq;
        this.start_date = start_date;
        this.end_date = end_date;
        this.instructions = instructions;
        this.log_medications = loadLogMedications();
    }

    private List<LogMedication> loadLogMedications() {
        // Load medication logs from the database
        LogMedicationDAO logMedicationDAO = new LogMedicationDAO();
        try {
            return logMedicationDAO.getLogMedicationsByMedicationId(this.id);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

    }

    // ===== Getters and Setters =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatient_id() { return patient_id; }
    public void setPatient_id(int patient_id) { this.patient_id = patient_id; }

    public String getName_medication() { return name_medication; }
    public void setName_medication(String name_medication) { this.name_medication = name_medication; }

    public String getDose() { return dose; }
    public void setDose(String dose) { this.dose = dose; }

    public Frequency getFreq() { return freq; }
    public void setFreq(Frequency freq) { this.freq = freq; }

    public LocalDate getStart_date() { return start_date; }
    public void setStart_date(LocalDate start_date) { this.start_date = start_date;}

    public LocalDate getEnd_date() { return end_date; }
    public void setEnd_date(LocalDate end_date) { this.end_date = end_date; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public List<LogMedication> getLogMedications() { return this.log_medications; }
    public void setLogMedications(List<LogMedication> log_medications) { this.log_medications = log_medications; }


    @Override
    public String toString() {
        return "Medication{" +
                "id=" + id +
                ", patient_id=" + patient_id +
                ", name_medication='" + name_medication + '\'' +
                ", dose=" + dose +
                ", freq=" + freq +
                ", start_date=" + start_date +
                ", end_date=" + end_date +
                ", instructions='" + instructions + '\'' +
                '}';
    }


    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return (today.isEqual(start_date) || today.isAfter(start_date)) &&
               (today.isEqual(end_date) || today.isBefore(end_date));
    }



    // Function that create a list of LogMedication for this medication
    // based on frequency and start_date and end_date
    public java.util.List<LogMedication> generateLogMedications() {
        java.util.List<LogMedication> logMedications = new java.util.ArrayList<>();
        LocalDate currentDate = start_date;

        int[] hours = freq.getHours();

        while (!currentDate.isAfter(end_date)) {
            for (int hour : hours) {
                LogMedication log = new LogMedication();
                log.setMedication_id(this.id);
                log.setDateAndTime(currentDate.atTime(hour, 0));
                log.setTaken(false);
                logMedications.add(log);
            }
            currentDate = currentDate.plusDays(1);
        }
        this.log_medications = logMedications;
        return logMedications;
    }




}
