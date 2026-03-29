package com.example.kai

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AppInfo(
    val name: String,
    val packageName: String,
    val version: String,
    val desc: String,
    val iconRes: Int
)

val appList = listOf(
    AppInfo("小x分身", "com.bly.dkplat", "理论通杀", "解锁本地会员|去广告", R.drawable.dkplat),
    AppInfo("安卓清理君", "com.magicalstory.cleaner", "理论通杀", "解锁本地会员|免登录", R.drawable.cleaner),
    AppInfo("Fake Location", "com.lerist.fakelocation", "1.3.1.9", "解锁专业版", R.drawable.fakelocation),
    AppInfo("多邻国", "com.duolingo", "6.71.2", "去广告|MAX会员|无限红心", R.drawable.duolingo),
    AppInfo("TapTap", "com.taptap", "买断通杀", "伪造购买|破解授权", R.drawable.taptap),
    AppInfo("葫芦侠", "com.huluxia.gametools", "理论通杀", "解锁主题|恢复评论", R.drawable.huluxia),
    AppInfo("一木记账", "com.wangc.bill", "理论通杀", "解锁本地会员", R.drawable.wangc),
    AppInfo("小白录屏", "com.xiaobai.screen.record", "理论通杀", "解锁本地会员", R.drawable.xiaobai),
    AppInfo("快对作业", "com.kuaiduizuoye.scan", "6.77.0", "解锁VIP|去广告|图片导出", R.drawable.kuaidui),
    AppInfo("绿茶VPN", "com.abglvcha.main", "2.6.7", "解锁钻石VIP", R.drawable.abglvcha),
    AppInfo("Soul", "com.soulapp", "理论通杀", "去聊天限制|消息撤回", R.drawable.soul),
    AppInfo("倒数日", "com.clover.daysmatter", "理论通杀", "解锁会员", R.drawable.daysmatter)
)

@Composable
fun AppsListScreen() {
    val ctx = LocalContext.current
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("适配应用", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)
        Spacer(Modifier.height(16.dp))

        LazyColumn(Modifier.fillMaxSize()) {
            items(appList, key = { it.packageName }) { app ->
                AppItem(app, colors) {
                    val intent = ctx.packageManager.getLaunchIntentForPackage(app.packageName)
                    if (intent != null) {
                        ctx.startActivity(intent)
                    } else {
                        Toast.makeText(ctx, "无法启动 ${app.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

@Composable
private fun AppItem(app: AppInfo, colors: ColorScheme, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(app.iconRes),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(app.name, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.onSurface)
                Text(app.packageName, fontSize = 12.sp, color = colors.onSurfaceVariant)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.primaryContainer
                    ) {
                        Text(
                            app.version,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(app.desc, fontSize = 12.sp, color = colors.onSurfaceVariant)
                }
            }
        }

        Divider(
            modifier = Modifier.padding(start = 62.dp),
            thickness = 0.5.dp,
            color = colors.onSurfaceVariant.copy(0.2f)
        )
    }
}
