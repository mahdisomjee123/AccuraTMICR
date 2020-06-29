package com.accurascan.accuraocr.sample;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.accurascan.accuraocr.sample.adapter.BarCodeTypeListAdapter;
import com.accurascan.ocr.mrz.CameraView;
import com.accurascan.ocr.mrz.interfaces.OcrCallback;
import com.accurascan.ocr.mrz.model.BarcodeTypeSelection;
import com.accurascan.ocr.mrz.model.OcrData;
import com.accurascan.ocr.mrz.model.PDF417Data;
import com.accurascan.ocr.mrz.model.RecogResult;
import com.accurascan.ocr.mrz.motiondetection.SensorsActivity;
import com.docrecog.scan.RecogType;

import java.lang.ref.WeakReference;
import java.util.List;

public class OcrActivity extends SensorsActivity implements OcrCallback {

    private CameraView cameraView;
    private View viewLeft, viewRight, borderFrame;
    private TextView tvTitle, tvScanMessage, btn_barcode_selection;
    private ImageView imageFlip;
    private int cardId;
    private int countryId;
    RecogType recogType;
    Dialog types_dialog;

    private static class MyHandler extends Handler {
        private final WeakReference<OcrActivity> mActivity;

        public MyHandler(OcrActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            OcrActivity activity = mActivity.get();
            if (activity != null) {
                String s = "";
                if (msg.obj instanceof String) {
                    s = (String) msg.obj;
                }
                switch (msg.what) {
                    case 0:
                        activity.tvTitle.setText(s);
                        break;
                    case 1:
                        activity.tvScanMessage.setText(s);
                        break;
                    case 2:
                        if (activity.cameraView != null)
                            activity.cameraView.flipImage(activity.imageFlip);
                        break;
                    default:
                        break;
                }
            }
            super.handleMessage(msg);
        }
    }

    private Handler handler = new MyHandler(this);/*new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            String s = "";
            if (msg.obj != null && msg.obj instanceof String) {
                s = (String) msg.obj;
            }
            switch (msg.what){
                case 0: tvTitle.setText(s);break;
                case 1: tvScanMessage.setText(s);break;
                case 2: if (cameraView != null) cameraView.flipImage(imageFlip); break;
                default: break;
            }
            super.handleMessage(msg);
        }
    };*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemeNoActionBar);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // Hide the window title.
        setContentView(R.layout.ocr_activity);
        init();

        recogType = RecogType.detachFrom(getIntent());
        cardId = getIntent().getIntExtra("card_id", 0);
        countryId = getIntent().getIntExtra("country_id", 0);

        Rect rectangle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        int statusBarHeight = rectangle.top;
        int contentViewTop =
                window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
        int titleBarHeight = contentViewTop - statusBarHeight;

        RelativeLayout linearLayout = findViewById(R.id.ocr_root); // layout width and height is match_parent
        cameraView = new CameraView(this);
        if (recogType == RecogType.OCR || recogType == RecogType.DL_PLATE) {
            cameraView.setCountryId(countryId)
                    .setCardId(cardId);

        } else if (recogType == RecogType.PDF417) {
            cameraView.setCountryId(countryId);
        }
        cameraView.setRecogType(recogType)
                .setView(linearLayout)
                .setOcrCallback(this)
                .setTitleBarHeight(titleBarHeight)
                .init();
        if (recogType == RecogType.BARCODE) setTypes_dialog();
    }

    private void init() {
        viewLeft = findViewById(R.id.view_left_frame);
        viewRight = findViewById(R.id.view_right_frame);
        borderFrame = findViewById(R.id.border_frame);
        tvTitle = findViewById(R.id.tv_title);
        tvScanMessage = findViewById(R.id.tv_scan_msg);
        imageFlip = findViewById(R.id.im_flip_image);
        btn_barcode_selection = findViewById(R.id.select_type);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (cameraView != null) cameraView.onWindowFocusUpdate(hasFocus);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraView != null) cameraView.onResume();
    }

    @Override
    protected void onPause() {
        if (cameraView != null) cameraView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (cameraView != null) cameraView.onDestroy();
        super.onDestroy();
        Runtime.getRuntime().gc();
    }

    /**
     * To update your border frame according to width and height
     * it's different for different card
     * Call {@link CameraView#startOcrScan(boolean)} method to start camera preview
     *
     * @param width    border layout width
     * @param height   border layout height
     */
    @Override
    public void onUpdateLayout(int width, int height) {
        if (cameraView != null) cameraView.startOcrScan(false);
        ViewGroup.LayoutParams layoutParams = borderFrame.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        borderFrame.setLayoutParams(layoutParams);
        ViewGroup.LayoutParams lpRight = viewRight.getLayoutParams();
        lpRight.height = height;
        viewRight.setLayoutParams(lpRight);
        ViewGroup.LayoutParams lpLeft = viewLeft.getLayoutParams();
        lpLeft.height = height;
        viewLeft.setLayoutParams(lpLeft);

        findViewById(R.id.ocr_frame).setVisibility(View.VISIBLE);

        if (recogType == RecogType.BARCODE) btn_barcode_selection.setVisibility(View.VISIBLE);
        else btn_barcode_selection.setVisibility(View.GONE);
    }

    /**
     * Override this method after scan complete to get data from document
     *
     * @param result is scanned card data
     *  result instance of {@link OcrData} if recog type is {@link com.docrecog.scan.RecogType#OCR}
     *              or {@link com.docrecog.scan.RecogType#DL_PLATE}
     *  result instance of {@link RecogResult} if recog type is {@link com.docrecog.scan.RecogType#MRZ}
     *  result instance of {@link PDF417Data} if recog type is {@link com.docrecog.scan.RecogType#PDF417}
     *  result instance of {@link String} if recog type is {@link com.docrecog.scan.RecogType#BARCODE}
     *
     */
    @Override
    public void onScannedComplete(Object result) {
        Log.e("TAG", "onScannedComplete: ");
        Intent intent = new Intent(this, OcrResultActivity.class);
        if (result != null) {
            if (result instanceof OcrData) {
                OcrData.setOcrResult((OcrData) result);
                if (recogType == RecogType.OCR) {
                    /**
                     * @recogType is {@link com.docrecog.scan.RecogType#OCR}*/
                    RecogType.OCR.attachTo(intent);
                } else if (recogType == RecogType.DL_PLATE) {
                    /**
                     * @recogType is {@link com.docrecog.scan.RecogType#DL_PLATE}*/
                    RecogType.DL_PLATE.attachTo(intent);
                }
                startActivityForResult(intent, 101);
            } else if (result instanceof RecogResult) {
                /**
                 *  @recogType is {@link com.docrecog.scan.RecogType#MRZ}*/
                RecogResult.setRecogResult((RecogResult) result);
                RecogType.MRZ.attachTo(intent);
                startActivityForResult(intent, 101);
            } else if (result instanceof PDF417Data) {
                /**
                 *  @recogType is {@link com.docrecog.scan.RecogType#PDF417}*/
                PDF417Data.setPDF417Result((PDF417Data) result);
                RecogType.PDF417.attachTo(intent);
                startActivityForResult(intent, 101);
            } else if (result instanceof String) {
                /**
                 *  @recogType is {@link com.docrecog.scan.RecogType#BARCODE}*/
                setResultDialog((String) result);
            }
        } else Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
    }

    /**
     * @param title   to display scan card message(is front/ back card of the #cardName)
     *                null if title is not available.
     * @param message to display process message.
     *                null if message is not available
     * @param isFlip  to set your customize animation after complete front scan
     */
    @Override
    public void onProcessUpdate(String title, String message, boolean isFlip) {
        Message message1 = new Message();
        message1.what = -1;
        if (title != null) {
            message1.what = 0;
            message1.obj = title;
            handler.sendMessage(message1);
//            tvTitle.setText(title);
        }
        if (message != null) {
            message1 = new Message();
            message1.what = 1;
            message1.obj = message;
            handler.sendMessage(message1);
//            tvScanMessage.setText(message);
        }
        if (isFlip) {
            message1 = new Message();
            message1.what = 2;
            handler.sendMessage(message1);//  to set default animation or remove this line to set your customize animation
        }

    }

    @Override
    public void onError(String errorMessage) {
        // stop ocr if failed
        tvScanMessage.setText(errorMessage);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 101) {
                Runtime.getRuntime().gc();
                if (cameraView != null) cameraView.startOcrScan(true);
            }
        }
    }

    private void setResultDialog(final String output) {
        Runnable runnable = new Runnable() {
            public void run() {
                AlertDialog.Builder dialog = new AlertDialog.Builder(OcrActivity.this);
                dialog.setTitle("Barcode Result");
                dialog.setMessage(output);
                dialog.setCancelable(false);
                dialog.setNegativeButton("Retry", (dialog1, which) -> {
                    if (cameraView != null) cameraView.onResume();
                });
                dialog.setPositiveButton("Ok", (dialog1, which) -> {
                    onBackPressed();
                });
                try {
                    dialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        runOnUiThread(runnable);

    }

    int mposition = 0;

    private void setTypes_dialog() {
        btn_barcode_selection.setOnClickListener(v -> {
            if (cameraView != null) cameraView.onPause();
            types_dialog.show();
        });
        List<BarcodeTypeSelection> CODE_NAMES = BarcodeTypeSelection.CODE_NAMES;
        types_dialog = new Dialog(this);
        types_dialog.setContentView(R.layout.dialog_barcode_type);
        types_dialog.setCanceledOnTouchOutside(false);
        types_dialog.setOnKeyListener((dialog, keyCode, event) -> {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                types_dialog.cancel();
            }
            return true;
        });
        types_dialog.setOnCancelListener(dialog -> {
            if (cameraView != null) cameraView.onResume();
        });

        ListView listView = types_dialog.findViewById(R.id.typelv);

        BarCodeTypeListAdapter adapter = new BarCodeTypeListAdapter(this, CODE_NAMES);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                for (int i = 0; i < CODE_NAMES.size(); i++) {
                    CODE_NAMES.get(i).isSelected = i == position;
                }
                adapter.notifyDataSetChanged();
                mposition = position;
                cameraView.setBarcodeFormat(CODE_NAMES.get(mposition).formatsType);
                types_dialog.cancel();
            }

        });

    }

}