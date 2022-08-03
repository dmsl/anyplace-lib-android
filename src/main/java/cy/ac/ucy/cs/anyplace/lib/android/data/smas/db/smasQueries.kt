package cy.ac.ucy.cs.anyplace.lib.android.data.smas.db

import androidx.sqlite.db.SimpleSQLiteQuery

/**
 * SMAS QUERIES:
 *
 * These are not supported (yet) through Room, but are supported through Raw SQLite.
 */
object smasQueries {

  /**
   * Surface Global Algorithm : NN-approx-geofrequency-ranked-closest-past
   * Use '?' for each variable that appears in the text
   *
   * Some adaptations from original SMAS query:
   * - FINGERPRINT_OBJECT does not exists, so the FINGERPRINT table is used
   *
   */
  fun global_algo3(uid: String, modelid: Int, buid: String) : SimpleSQLiteQuery {
    return SimpleSQLiteQuery(
            """
    -- NN-approx-geofrequency-ranked
    -- Descr: Executes 1NN but doesnt require all objects to be present
    -- same as before but different ranking function
    SELECT F.flid, F.X, F.Y, F.deck, (SELECT IFNULL(COUNT(*),0) FROM
        ( -- simulate except all operator with rowid
        SELECT ROW_NUMBER() OVER (PARTITION BY FLT.oid) AS RowNum, FLT.oid FROM FINGERPRINT_LOCALIZE_TEMP FLT WHERE FLT.uid=?
        EXCEPT
        SELECT ROW_NUMBER() OVER (PARTITION BY FO.oid) AS RowNum, FO.oid FROM FINGERPRINT FO WHERE FO.flid=F.flid
        )
    ) AS dissimilarity, (SELECT IFNULL(AVG(weight),1) FROM
        ( -- simulate except all operator with rowid
        SELECT ROW_NUMBER() OVER (PARTITION BY FLT.oid) AS RowNum, FLT.oid, OF.weight as weight FROM FINGERPRINT_LOCALIZE_TEMP FLT, OBJECT_FREQUENCY OF WHERE  FLT.oid =  OF.oid and FLT.uid=?
        EXCEPT
        SELECT ROW_NUMBER() OVER (PARTITION BY FO.oid) AS RowNum, FO.oid, OF.weight  as weight FROM FINGERPRINT FO,  OBJECT_FREQUENCY OF WHERE   FO.oid =  OF.oid and FO.flid=F.flid
        )
    ) AS weight
    FROM FINGERPRINT F
    WHERE F.modelid=? and F.buid=?
    GROUP BY F.X, F.Y, F.deck -- keep only x,y,deck
    HAVING dissimilarity < (SELECT COUNT(*) from FINGERPRINT_LOCALIZE_TEMP FLT WHERE  FLT.uid=?)
    ORDER BY dissimilarity, weight ASC
    LIMIT 1; -- sort by newest match to oldest MATCH
""",
            arrayOf<Any>(uid, uid, modelid, buid, uid))
  }


  // Bounding Rectangle Queries
  // 1 = 111 km (or 60 nautical miles) => ROUND(0)
  // 0.1 = 11.1 km => ROUND(1)
  // 0.01 = 1.11 km (2 decimals, km accuracy) => ROUND(2)
  // 0.001 = 111 meters => ROUND(3)
  // 0.0001 = 11 meters => ROUND(4)
  private val smas_db_messages_bound_meters    = 0.001  // bounding rectangle around query  mdelivery#4
  private val smas_db_location_bound_meters    = 0.01   // 1000 meter from prior location.
  private val smas_db_location_bound_rounding  = 4      // then do counting in 10meter boxes

  /**
   * Surface Radius Algorithm - NN-approx-geofrequency-ranked-closest-past-within-bounding-rectangle
   * Use '?' for each variable that appears in the text
   *
   * Some adaptations from original SMAS query:
   * - FINGERPRINT_OBJECT does not exists, so the FINGERPRINT table is used
   *
   */
  fun local_algo4(prevX: Double, prevY: Double, prevDeck: Int,
                  uid: String, modelid: Int, buid: String) : SimpleSQLiteQuery {
    return SimpleSQLiteQuery(
            """
    -- NN-approx-geofrequency-ranked
    -- Descr: Executes 1NN but doesnt require all objects to be present
    -- same as before but different ranking function
                     
    SELECT F.flid, F.X, F.Y, F.deck,
                     ABS(x - ?) as xDiff, ABS(y - ?) as yDiff, ABS(deck - ?) as deckDiff,
                     (SELECT IFNULL(COUNT(*),0) FROM
        ( -- simulate except all operator with rowid
        SELECT ROW_NUMBER() OVER (PARTITION BY FLT.oid) AS RowNum, FLT.oid FROM FINGERPRINT_LOCALIZE_TEMP FLT WHERE FLT.uid=?
        EXCEPT
        SELECT ROW_NUMBER() OVER (PARTITION BY FO.oid) AS RowNum, FO.oid FROM FINGERPRINT FO WHERE FO.flid=F.flid
        )
    ) AS dissimilarity, (SELECT IFNULL(AVG(weight),1) FROM
        ( -- simulate except all operator with rowid
        SELECT ROW_NUMBER() OVER (PARTITION BY FLT.oid) AS RowNum, FLT.oid, OF.weight as weight FROM FINGERPRINT_LOCALIZE_TEMP FLT, OBJECT_FREQUENCY OF WHERE  FLT.oid =  OF.oid and FLT.uid=?
        EXCEPT
        SELECT ROW_NUMBER() OVER (PARTITION BY FO.oid) AS RowNum, FO.oid, OF.weight  as weight FROM FINGERPRINT FO,  OBJECT_FREQUENCY OF WHERE   FO.oid =  OF.oid and FO.flid=F.flid
        )
    ) AS weight
    FROM FINGERPRINT F
    WHERE F.modelid=? and F.buid=? and
                (x between ? - ? and ? + ?) and
                (y between ? - ? and ? + ?)
                and (deck between ? - 1 and ? + 1)
    GROUP BY ROUND(F.X,?), ROUND(F.Y,?), F.deck -- keep only x,y,deck rounded by 10m bounding box
    HAVING dissimilarity < (SELECT COUNT(*) from FINGERPRINT_LOCALIZE_TEMP FLT WHERE  FLT.uid=?)
    ORDER BY dissimilarity, weight, ABS(x - ?) + ABS(y - ?) + ABS(deck - ?)  ASC
    LIMIT 1; -- sort by newest match to oldest MATCH
""",
            arrayOf<Any>(prevX, prevY, prevDeck, uid, uid, modelid, buid,
                    prevX, smas_db_location_bound_meters, prevX, smas_db_location_bound_meters,
                    prevY, smas_db_location_bound_meters, prevY, smas_db_location_bound_meters,
                    prevDeck, prevDeck,
                    smas_db_location_bound_rounding,
                    smas_db_location_bound_rounding,
                    uid,
                    prevX, prevY, prevDeck
            ))
  }

}