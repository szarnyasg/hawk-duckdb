package org.eclipse.hawk.duckdb;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.eclipse.hawk.core.graph.IGraphTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Companion object to a {@link DuckDatabase} that keeps track of the single transaction
 * that we handle in it.
 */
public class DuckTransaction implements IGraphTransaction {

	private static final Logger LOGGER = LoggerFactory.getLogger(DuckTransaction.class);

	private final Connection duckDB;
	private boolean active;

	public DuckTransaction(Connection duckDB) {
		this.duckDB = duckDB;
		this.active = true;
	}

	@Override
	public void success() {
		try {
			if (DuckDatabase.DEBUG_SQL) {
				System.out.println("COMMIT");
			}
			duckDB.commit();
			active = false;
		} catch (SQLException e) {
			LOGGER.error("Failed to commit", e);
		}
	}

	@Override
	public void failure() {
		try {
			if (DuckDatabase.DEBUG_SQL) {
				System.out.println("ROLLBACK");
			}
			duckDB.rollback();
			active = false;
		} catch (SQLException e) {
			LOGGER.error("Failed to rollback", e);
		}
	}

	@Override
	public void close() {
		// no-op in DuckDB: we don't want to actually close the connection!
	}

	public void begin() {
		if (!active) {
			try (Statement stmt = duckDB.createStatement()) {
				final String sql = "BEGIN TRANSACTION";
				if (DuckDatabase.DEBUG_SQL) {
					System.out.println(sql);
				}
				stmt.execute(sql);
				active = true;
			} catch (SQLException e) {
				LOGGER.error("Failed to start a transaction", e);
			}
		}
	}

}
