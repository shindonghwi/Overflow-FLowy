package at.overflow.flowy.DB;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataAdapter
{
    protected static final String TAG = "DataAdapter";

    // TODO : TABLE 이름을 명시해야함
    protected static final String TABLE_NAME = "FlowyBus_Sheet0";

    private final Context mContext;
    private SQLiteDatabase mDb;
    private DataBaseHelper mDbHelper;

    public DataAdapter(Context context)
    {
        this.mContext = context;
        mDbHelper = new DataBaseHelper(mContext);
    }

    public DataAdapter createDatabase() throws SQLException
    {
        try
        {
            mDbHelper.createDataBase();
        }
        catch (IOException mIOException)
        {
            Log.e(TAG, mIOException.toString() + "  UnableToCreateDatabase");
            throw new Error("UnableToCreateDatabase");
        }
        return this;
    }

    public DataAdapter open() throws SQLException
    {
        try
        {
            mDbHelper.openDataBase();
            mDbHelper.close();
            mDb = mDbHelper.getReadableDatabase();
        }
        catch (SQLException mSQLException)
        {
            Log.e(TAG, "open >>"+ mSQLException.toString());
            throw mSQLException;
        }
        return this;
    }

    public void close()
    {
        mDbHelper.close();
    }

    public List<BusDataModel> getTableData(String latitudeMin, String longitudeMin, String latitudeMax, String longitudeMax)
    {
        // Table 이름 -> antpool_bitcoin 불러오기
        String sql ="SELECT `routeName` FROM `FlowyBus_Sheet0` WHERE " +
                "`x` >= '" + longitudeMin + "' " + " AND " +
                "`x` <= '" + longitudeMax + "' " + " AND " +
                "`Y` >= '" + latitudeMin + "' " + " AND " +
                "`Y` <= '" + latitudeMax + "' group by routeName";

        // 모델 넣을 리스트 생성
        List<BusDataModel> busDataModelList = new ArrayList<BusDataModel>();

        // TODO : 모델 선언
        BusDataModel busDataModel = null;

        Cursor mCur = mDb.rawQuery(sql, null);
        if (mCur!=null)
        {
            // 칼럼의 마지막까지
            while( mCur.moveToNext() ) {

                // TODO : 커스텀 모델 생성
                busDataModel = new BusDataModel(
                        "",
                        mCur.getString(0),
                        "",
                        "",
                        "",
                        ""
                );

                // 리스트에 넣기
                busDataModelList.add(busDataModel);
            }

        }
        return busDataModelList;
    }

}