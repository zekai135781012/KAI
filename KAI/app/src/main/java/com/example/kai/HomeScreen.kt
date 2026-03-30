package com.example.kai

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(onShowAbout: () -> Unit) {
    val ctx = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val adaptedCount = appList.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("KAI", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)

        Column(Modifier.fillMaxWidth()) {
            InfoItem("设备名称", Build.MODEL)
            ThinDivider()
            InfoItem("系统版本", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            ThinDivider()
            InfoItem("适配应用数量", adaptedCount.toString())
        }

        ThinDivider(Modifier.fillMaxWidth())

        Text("公告 & 更新日志", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.onSurface)
        ScrollableBox(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 140.dp)
        ) {
            Text(
                "• 2026.03.30 新增文本替换+弹窗屏蔽功能，精准适配应用，规则独立生效不干扰\n" +
                "• 2026.03.26 新增适配「多邻国」\n" +
                "  解锁MAX会员、去除广告、无限红心\n" +
                "• 2026.03.12 新增适配「快对作业」\n" +
                "  解锁VIP、去广告、图片解密导出、解除截屏限制\n" +
                "• 2026.03.11 优化界面布局\n" +
                "• 2026.03.10 新增关于模块页面，完善功能说明\n" +
                "• 2026.03.09 新增适配「小白录屏」「倒数日」\n" +
                "  解锁会员、去除广告等核心Hook\n" +
                "• 2026.03.08 初始版本发布，支持小X分身、安卓清理君等应用",
                fontSize = 14.sp, color = colors.onSurfaceVariant, lineHeight = 20.sp
            )
        }

        ThinDivider(Modifier.fillMaxWidth())

        FuncItem(Icons.Default.Info, "关于模块", "查看介绍与声明", onShowAbout)
        ThinDivider(Modifier.fillMaxWidth())
        FuncItem(Icons.Default.Person, "QQ群聊", "交流反馈") {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                "https://qun.qq.com/universal-share/share?ac=1&authKey=tT4Jqq36aIUxIViNKSImeHeLs8kge3lgAAPrGeZDhpUCnWjXzLfiPbybVaGQ0VuL&busi_data=eyJncm91cENvZGUiOiI3MDk0OTY5NTciLCJ0b2tlbiI6Ink0UjFoU243RmpXY3VVeVkvamw5ZE5kSmZWN0k0bGN4ZEZFdU8xR2V6b2dzMVZlSzdpaFpvOWY1enJiekx5NlAiLCJ1aW4iOiIyMDI2ODc1NDA1In0%3D&data=ihTK1i_K9xV-ctPsiIhIfgZ8RF192JZrWAKAeELPyQAYUsArldNcB8TKx4UVMDr05QK48H16oAboZpbzf8W5LA&svctype=4&tempid=h5_group_info"
            )))
        }
        
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, null, tint = colors.primary)
            Spacer(Modifier.width(8.dp))
            Text("关于模块", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
        }

        Text("KAI", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.primary)
        Text("本模块仅供学习研究，仅限测试使用。", fontSize = 14.sp, color = colors.onSurfaceVariant, lineHeight = 20.sp)

        ThinDivider(Modifier.fillMaxWidth())

        AuthorCard()

        ThinDivider(Modifier.fillMaxWidth())

        TextSection("免责声明", "本模块仅用于学习交流，请勿用于商业用途。\n\n使用本模块造成的任何后果，作者不承担责任。\n\n侵权联系：3054147978@qq.com")

        ThinDivider(Modifier.fillMaxWidth())

        Text("特别鸣谢", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.onSurface)
        
        ScrollableBox(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                credits.forEach { credit ->
                    CreditItem(credit.drawable, credit.name, credit.desc)
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ScrollableBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    val fadeColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .verticalScroll(scrollState)
            .drawWithContent {
                drawContent()
                
                if (scrollState.value < scrollState.maxValue) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, fadeColor.copy(0.6f)),
                            startY = size.height - 20.dp.toPx(),
                            endY = size.height
                        ),
                        topLeft = Offset(0f, size.height - 20.dp.toPx()),
                        size = Size(size.width, 20.dp.toPx())
                    )
                }
                
                if (scrollState.value > 0) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(fadeColor.copy(0.4f), Color.Transparent),
                            startY = 0f,
                            endY = 12.dp.toPx()
                        ),
                        size = Size(size.width, 12.dp.toPx())
                    )
                }
            }
    ) {
        content()
    }
}

private data class Credit(val drawable: Int, val name: String, val desc: String)

private val credits = listOf(
    Credit(R.drawable.mt, "MT论坛", "提供各种学习资源与帮助，感谢各位大佬😘"),
    Credit(R.drawable.wm, "挽梦遗酒", "提供简单的关键词弹窗屏蔽方法"),
        Credit(R.drawable.dusheling, "dusheling", "参考多邻国MXA等解锁方法"),
    Credit(R.drawable.octobersama, "OctoberSama", "参考OctoberSama的taptap hook思路，但由于没有tap游戏的安装包无法测试是否可用"),
    Credit(R.drawable.jiguro, "JiGuro", "模块参考JiGuro的快对作业以及葫芦侠的hook方法")
)

@Composable
private fun ThinDivider(modifier: Modifier = Modifier) {
    Divider(
        modifier = modifier.padding(vertical = 6.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2f)
    )
}

@Composable
private fun InfoItem(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Column {
        Text(label, fontSize = 15.sp, color = colors.onSurfaceVariant, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 17.sp, color = colors.onSurface)
    }
}

@Composable
private fun FuncItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = colors.primary, modifier = Modifier.size(24.dp))
        Column {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.onSurface)
            Text(desc, fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun AuthorCard() {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.zuozhe),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(32.dp))
        )
        Column {
            Text("探长鸽鸽", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Text("一位老爱瞎捣鼓的小白", fontSize = 16.sp, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun TextSection(title: String, content: String) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.onSurface)
        Spacer(Modifier.height(10.dp))
        Text(content, fontSize = 14.sp, color = colors.onSurfaceVariant, lineHeight = 20.sp)
    }
}

@Composable
private fun CreditItem(drawable: Int, name: String, desc: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = drawable),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Column {
            Text(name, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.onSurface)
            Text(desc, fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
    }
}
