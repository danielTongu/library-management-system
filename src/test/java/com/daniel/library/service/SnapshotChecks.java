package com.daniel.library.service;

import com.daniel.library.demo.SampleData;
import com.daniel.library.persistence.InMemoryLibraryRepository;
import com.daniel.library.ui.LibraryRecords;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Guards against repeated database reads and inconsistent reporting dates in a screen refresh. */
public class SnapshotChecks {
    public static void main(String[] args) {
        CountingRepository repository = new CountingRepository();
        SampleData.populate(repository);
        TestClock clock = new TestClock();
        Library library = new Library(new BigDecimal("0.50"), clock, repository);
        repository.reads = 0;
        LibraryRecords rows = LibraryRecords.load(library, "Ready");
        check(repository.reads == 1, "A complete workspace refresh must read storage exactly once");
        check(rows.loans().size() == 3 && rows.overview().size() == 12, "Every section comes from the snapshot");
        LibrarySnapshot snapshot = library.getSnapshot();
        var active = snapshot.records().loans().stream()
                .filter(loan -> loan.getLoanId().equals("sample-loan-active")).findFirst().orElseThrow();
        clock.date = LocalDate.of(2026, 11, 18);
        check(snapshot.calculateFine(active).equals(new BigDecimal("0.00")), "Report date remains fixed");
        check(!snapshot.isOverdue(active), "Status uses the same report date as the fine");
        check(library.getSnapshot().calculateFine(active).equals(new BigDecimal("1.50")), "New refresh uses new date");
        System.out.println("All snapshot consistency checks passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static class CountingRepository extends InMemoryLibraryRepository {
        private int reads;

        @Override
        public LibraryData load() {
            reads++;
            return super.load();
        }
    }

    private static class TestClock extends Clock {
        private LocalDate date = LocalDate.of(2026, 11, 3);

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant(), zone);
        }

        @Override
        public Instant instant() {
            return date.atStartOfDay(ZoneOffset.UTC).toInstant();
        }
    }
}
