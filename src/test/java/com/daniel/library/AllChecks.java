package com.daniel.library;

import com.daniel.library.demo.SampleDataChecks;
import com.daniel.library.model.ValidationChecks;
import com.daniel.library.persistence.PersistenceFailureChecks;
import com.daniel.library.service.ArchivingChecks;
import com.daniel.library.service.AvailabilityChecks;
import com.daniel.library.service.EditingChecks;
import com.daniel.library.service.FineChecks;
import com.daniel.library.service.SnapshotChecks;
import com.daniel.library.service.SummaryChecks;
import com.daniel.library.ui.TableFilterChecks;
import com.daniel.library.ui.CsvExportChecks;
import com.daniel.library.ui.DesktopChecks;

import java.util.List;

/** Runs local checks together. Live MySQL and native windows are deliberately excluded. */
public class AllChecks {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        List<Check> checks = List.of(new Check("Model validation", ValidationChecks::main),
                new Check("Fines and returns", FineChecks::main),
                new Check("Availability", AvailabilityChecks::main),
                new Check("Editing", EditingChecks::main),
                new Check("Archiving", ArchivingChecks::main),
                new Check("Overview totals", SummaryChecks::main),
                new Check("Snapshot consistency", SnapshotChecks::main),
                new Check("Persistence failures", PersistenceFailureChecks::main),
                new Check("Desktop data and tables", DesktopChecks::main),
                new Check("Desktop filters", TableFilterChecks::main),
                new Check("Shared samples", SampleDataChecks::main),
                new Check("CSV exports", CsvExportChecks::main));
        AssertionError failures = new AssertionError("One or more local check groups failed.");
        int passed = 0;
        for (Check check : checks) {
            try {
                check.run().execute(new String[0]);
                passed++;
            } catch (Exception | AssertionError exception) {
                System.err.println("FAILED: " + check.name() + " — " + exception.getMessage());
                failures.addSuppressed(exception);
            }
        }
        System.out.println("Local checks: " + passed + "/" + checks.size() + " groups passed.");
        if (passed != checks.size()) {
            throw failures;
        }
    }

    private record Check(String name, CheckMain run) {
    }

    @FunctionalInterface
    private interface CheckMain {
        void execute(String[] args) throws Exception;
    }
}
