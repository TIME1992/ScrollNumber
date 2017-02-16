package top.wuhaojie.scrollnumber;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import top.wuhaojie.library.MultiScrollNumber;

public class MainActivity extends AppCompatActivity {
    private int number = 199;
    int i = 0;
    private String lastText = "000000000";
    private String curText = "123456789";
    private String text = lastText;
    private MultiScrollNumber scrollNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        lastText = "199万";
        //只滚一轮情况下
        scrollNumber = (MultiScrollNumber) findViewById(R.id.scroll_number);

        scrollNumber.setTextColors(new int[]{R.color.blue01, R.color.red01,
                R.color.green01, R.color.purple01});
//        scrollNumber.setTextSize(64);

//        scrollNumber.setNumber(64, 2048);
//        scrollNumber.setInterpolator(new DecelerateInterpolator());
//        scrollNumber.setScrollNumberBackgroundResource(R.mipmap.singlebox_unselected);
        scrollNumber.setTextSize(40);
        scrollNumber.setTextFont("myfont.ttf");
        scrollNumber.setUnitTextFont("Helvetica_LT45_Light.ttf");
        scrollNumber.setScollAnimationMode(MultiScrollNumber.Mode.START_FIRST_ARRIVAL_LAST);
        scrollNumber.setNumber(lastText);
//        scrollNumber.play();

        Button button = (Button) findViewById(R.id.play);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                scrollNumber.play();
//                if(text.equals(lastText)) {
//                    scrollNumber.setNumberWithAnimation(curText);
//                    text = curText;
//                } else {
//                    scrollNumber.setNumberWithAnimation(lastText);
//                    text = lastText;
//                }
            }
        });
    }
}
