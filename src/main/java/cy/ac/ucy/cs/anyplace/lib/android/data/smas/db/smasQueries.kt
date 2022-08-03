package cy.ac.ucy.cs.anyplace.lib.android.data.smas.db

import androidx.sqlite.db.SimpleSQLiteQuery

/**
 * These are not supported (yet) through Room, but are supported through SQLite.
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
  fun algo3(uid: String, modelid: Int, buid: String) : SimpleSQLiteQuery {
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



//   /**
//    *
//    */
//   fun algo4(uid: String, modelid: Int, buid: String) : SimpleSQLiteQuery {
//     return SimpleSQLiteQuery(
//             """
// """,
//             arrayOf<Any>(uid, uid, modelid, buid, uid))
//   }



  // CLR:PM
  // /**
  //  *
  //  */
  // fun getQueryAlgo3(modelid: Int, buid: String) : SimpleSQLiteQuery {
  //   return SimpleSQLiteQuery(
  //           "    SELECT F.flid, F.X, F.Y, F.deck, (SELECT IFNULL(SUM(weight)/COUNT(*),0) FROM\n" +
  //                   "        ( -- simulate except all operator with rowid\n" +
  //                   "        SELECT ROW_NUMBER() OVER (PARTITION BY FLT.oid) AS RowNum, FLT.oid, OFR.weight as weight\n" +
  //                   "          FROM FINGERPRINT_LOCALIZE_TEMP FLT, OBJECT_FREQUENCY OFR WHERE  FLT.oid =  OFR.oid\n" +
  //                   "        EXCEPT\n" +
  //                   "        SELECT ROW_NUMBER() OVER (PARTITION BY FO.oid) AS RowNum, FO.oid, OFR.weight  as weight\n" +
  //                   "          FROM FINGERPRINT FO,  OBJECT_FREQUENCY OFR WHERE   FO.oid =  OFR.oid and FO.flid=F.flid\n" +
  //                   "        )\n" +
  //                   "    ) AS dissimilarity\n" +
  //                   "    FROM FINGERPRINT F\n" +
  //                   "    WHERE F.modelid=? and F.buid=? \n" +
  //                   "    GROUP BY F.X, F.Y, F.deck -- keep only x,y,deck\n" +
  //                   "    ORDER BY dissimilarity, time ASC\n" +
  //                   "    LIMIT 1; -- sort by newest match to oldest MATCH",
  //           arrayOf<Any>(modelid, buid))
// }

}