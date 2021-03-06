package com.github.dmn1k.table.diff;

import io.vavr.Function2;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import lombok.Value;

import static io.vavr.API.*;
import static io.vavr.Patterns.$None;
import static io.vavr.Patterns.$Some;

@Value
public class TableDiffResult {
    private Option<TableRow> newRow;
    private Option<TableRow> oldRow;
    private DiffType diffType;

    public static TableDiffResult create(Tuple2<Option<TableRow>, Option<TableRow>> rows,
                                         Function2<TableCell, TableCell, Boolean> columnComparisonFn) {
        return create(rows._1, rows._2, columnComparisonFn);
    }

    public static TableDiffResult create(Option<TableRow> optNewRow,
                                         Option<TableRow> optOldRow,
                                         Function2<TableCell, TableCell, Boolean> columnComparisonFn) {
        return Match(optNewRow).of(

                Case($None(), () -> Match(optOldRow).of(

                        Case($None(), () -> createUnchanged(Option.none())),
                        Case($Some($()), oldRow -> createDeleted(oldRow)))

                ),

                Case($Some($()), newRow -> Match(optOldRow).of(

                        Case($None(), () -> createNew(newRow)),
                        Case($Some($()), oldRow -> newRow.isSameAs(oldRow, columnComparisonFn)
                                ? createUnchanged(newRow)
                                : createChanged(newRow, oldRow)))

                )
        );
    }

    public static TableDiffResult createChanged(TableRow newRow, TableRow oldRow) {
        return new TableDiffResult(Option.of(newRow), Option.of(oldRow), DiffType.Changed);
    }

    public static TableDiffResult createUnchanged(TableRow row) {
        return createUnchanged(Option.of(row));
    }

    public static TableDiffResult createUnchanged(Option<TableRow> row) {
        return new TableDiffResult(row, row, DiffType.Unchanged);
    }

    public static TableDiffResult createNew(TableRow newRow) {
        return new TableDiffResult(Option.of(newRow), Option.none(), DiffType.New);
    }

    public static TableDiffResult createDeleted(TableRow deletedRow) {
        return new TableDiffResult(Option.none(), Option.of(deletedRow), DiffType.Deleted);
    }

    public String getPrimaryKey() {
        return newRow
                .orElse(oldRow)
                .map(TableRow::primaryKeyValue)
                .getOrElse("Neither new nor old row exists. This is odd.");
    }
}
