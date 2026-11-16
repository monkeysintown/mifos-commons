///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.persistence.relational.core.querydsl;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringPath;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/// PostgreSQL ltree operators for QueryDSL.
///
/// Supported operators and functions:
///
///   - `@>`: ltree contains (ancestor)
///   - `<@`: ltree is contained by (descendant)
///   - `~`: ltree matches lquery
///   - `||`: ltree concatenation
///   - `nlevel()`: number of levels in path
///   - `subpath()`: extract portion of path
///   - `text2ltree()`, `ltree2text()`: conversion between text and ltree
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MifosPersistenceRelationalPgLtreeExpressions {

    /// Tests whether `path` is an ancestor of (or equal to) `ancestorPath` (`path @> ancestorPath`).
    ///
    /// @param path the ltree path (cast to `ltree`)
    /// @param ancestorPath the path that must be contained in `path`
    /// @return the boolean expression
    public static BooleanExpression contains(StringPath path, String ancestorPath) {
        return Expressions.booleanTemplate(
                "cast({0} as ltree) @> cast({1} as ltree)", path, Expressions.constant(ancestorPath));
    }

    /// Tests whether `path` is an ancestor of (or equal to) `ancestorPath` (`path @> ancestorPath`).
    ///
    /// @param path the ltree path (cast to `ltree`)
    /// @param ancestorPath the path expression that must be contained in `path`
    /// @return the boolean expression
    public static BooleanExpression contains(StringPath path, Expression<String> ancestorPath) {
        return Expressions.booleanTemplate("cast({0} as ltree) @> cast({1} as ltree)", path, ancestorPath);
    }

    /// Tests whether `path` is a descendant of (or equal to) `descendantPath` (`path <@ descendantPath`).
    ///
    /// @param path the ltree path (cast to `ltree`)
    /// @param descendantPath the path that must contain `path`
    /// @return the boolean expression
    public static BooleanExpression containedBy(StringPath path, String descendantPath) {
        return Expressions.booleanTemplate(
                "cast({0} as ltree) <@ cast({1} as ltree)", path, Expressions.constant(descendantPath));
    }

    /// Tests whether `path` is a descendant of (or equal to) `descendantPath` (`path <@ descendantPath`).
    ///
    /// @param path the ltree path (cast to `ltree`)
    /// @param descendantPath the path expression that must contain `path`
    /// @return the boolean expression
    public static BooleanExpression containedBy(StringPath path, Expression<String> descendantPath) {
        return Expressions.booleanTemplate("cast({0} as ltree) <@ cast({1} as ltree)", path, descendantPath);
    }

    /// Tests whether `path` matches an lquery pattern (`path ~ lqueryPattern`).
    ///
    /// @param path the ltree path (cast to `ltree`)
    /// @param lqueryPattern the lquery pattern
    /// @return the boolean expression
    public static BooleanExpression matches(StringPath path, String lqueryPattern) {
        return Expressions.booleanTemplate("cast({0} as ltree) ~ {1}", path, Expressions.constant(lqueryPattern));
    }

    /// Returns the number of labels in `path` (`nlevel(path)`).
    ///
    /// @param path the ltree path (cast to `ltree`)
    /// @return the number of levels
    public static Expression<Integer> nlevel(StringPath path) {
        return Expressions.numberTemplate(Integer.class, "nlevel(cast({0} as ltree))", path);
    }

    /// Returns the subpath of `path` starting at position `start` up to the end (`subpath(path, start)`).
    ///
    /// @param path the ltree path (cast to `ltree`)
    /// @param start the zero-based start position; negative counts from the end
    /// @return the subpath
    public static Expression<String> subpath(StringPath path, int start) {
        return Expressions.stringTemplate("subpath(cast({0} as ltree), {1})", path, Expressions.constant(start));
    }

    /// Returns the subpath of `path` starting at `start` (`subpath(path, start, end)`).
    ///
    /// @param path the ltree path (cast to `ltree`)
    /// @param start the zero-based start position; negative counts from the end
    /// @param end the length of the subpath; negative leaves off that many labels from the end
    /// @return the subpath
    public static Expression<String> subpath(StringPath path, int start, int end) {
        return Expressions.stringTemplate(
                "subpath(cast({0} as ltree), {1}, {2})", path, Expressions.constant(start), Expressions.constant(end));
    }

    /// Concatenates two ltree paths (`path1 || path2`).
    ///
    /// @param path1 the first path (cast to `ltree`)
    /// @param path2 the path appended to `path1`
    /// @return the concatenated path
    public static Expression<String> concat(StringPath path1, String path2) {
        return Expressions.stringTemplate(
                "cast({0} as ltree) || cast({1} as ltree)", path1, Expressions.constant(path2));
    }

    /// Converts text to ltree (`text2ltree(text)`).
    ///
    /// @param text the text expression
    /// @return the ltree expression
    public static Expression<String> text2ltree(Expression<String> text) {
        return Expressions.stringTemplate("text2ltree({0})", text);
    }

    /// Converts an ltree path to text (`ltree2text(path)`).
    ///
    /// @param path the ltree path (cast to `ltree`)
    /// @return the text expression
    public static Expression<String> ltree2text(StringPath path) {
        return Expressions.stringTemplate("ltree2text(cast({0} as ltree))", path);
    }
}
