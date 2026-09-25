package com.softhome.core.designsystem.atom

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LocalContentColor
import com.softhome.core.designsystem.R

/**
 * Line-art icon wrapper for the bundled lucide-style vector drawables.
 *
 * Source: design/homeApp.pen frame `lk7jo` (Soft Monochrome Icon Set) -- all glyphs
 * are lucide, 24x24, stroke-width 2, round caps/joins, monochrome cream on charcoal.
 * Assets live as vector drawables in core/designsystem res/drawable.
 */
enum class LineIcon(val resId: Int) {
    Mail(R.drawable.mail),
    Globe(R.drawable.globe),
    Image(R.drawable.image),
    Music(R.drawable.music),
    Phone(R.drawable.phone),
    MessageCircle(R.drawable.message_circle),
    FileText(R.drawable.file_text),
    Camera(R.drawable.camera),
    CalendarDays(R.drawable.calendar_days),
    MapPin(R.drawable.map_pin),
    Settings(R.drawable.settings),
    Calculator(R.drawable.calculator),
    Clock(R.drawable.clock_3),
    Folder(R.drawable.folder),
    CloudSun(R.drawable.cloud_sun),
    Compass(R.drawable.compass),
    AlarmClock(R.drawable.alarm_clock),
    Play(R.drawable.play),
    Pause(R.drawable.pause),
    PenLine(R.drawable.pen_line),
    PhoneCall(R.drawable.phone_call),
    Sun(R.drawable.sun),
    Search(R.drawable.search),
    Mic(R.drawable.mic),
    ArrowUpRight(R.drawable.arrow_up_right),
    AppWindow(R.drawable.app_window),
    Square(R.drawable.square),

    // --- Warm Right Rail additions (home rail + music + drawer examples) ---
    Sparkles(R.drawable.sparkles),
    CircleDot(R.drawable.circle_dot),
    Send(R.drawable.send),
    Wind(R.drawable.wind),
    PanelLeft(R.drawable.panel_left),
    SkipBack(R.drawable.skip_back),
    SkipForward(R.drawable.skip_forward),
    Disc3(R.drawable.disc_3),
    Tent(R.drawable.tent),
    Images(R.drawable.images),
    BookOpen(R.drawable.book_open),
    WalletCards(R.drawable.wallet_cards),
    Shield(R.drawable.shield),
    Bot(R.drawable.bot),
    Contact(R.drawable.contact),
    Box(R.drawable.box),
    FolderSymlink(R.drawable.folder_symlink),
    FolderOpen(R.drawable.folder_open),
    GraduationCap(R.drawable.graduation_cap),
    HeartHandshake(R.drawable.heart_handshake),
    ShoppingBag(R.drawable.shopping_bag),
    MessagesSquare(R.drawable.messages_square),
    Users(R.drawable.users),
    Utensils(R.drawable.utensils),
    Wallet(R.drawable.wallet),
    MoreVertical(R.drawable.ellipsis_vertical),

    // --- P2 additions (widget rows + folder) ---
    Battery(R.drawable.battery),
    HardDrive(R.drawable.hard_drive),

    // --- P3 additions (context menu + settings) ---
    Info(R.drawable.info),
    Trash2(R.drawable.trash_2),
    ChevronRight(R.drawable.chevron_right),
    ArrowUp(R.drawable.arrow_up),
    ArrowDown(R.drawable.arrow_down),
    X(R.drawable.x),
    ExternalLink(R.drawable.external_link),

    // --- P4 additions (per-app drawer glyphs, ported from .pen `ciHU3`) ---
    MessageSquare(R.drawable.message_square),
    ContactRound(R.drawable.contact_round),
    Video(R.drawable.video),
    Network(R.drawable.network),
    PhoneOff(R.drawable.phone_off),
    Share2(R.drawable.share_2),
    Pin(R.drawable.pin),
    Music2(R.drawable.music_2),
    Gamepad2(R.drawable.gamepad_2),
    Joystick(R.drawable.joystick),
    UsersRound(R.drawable.users_round),
    Languages(R.drawable.languages),
    NotebookPen(R.drawable.notebook_pen),
    CloudUpload(R.drawable.cloud_upload),
    Settings2(R.drawable.settings_2),
    Palette(R.drawable.palette),
    CreditCard(R.drawable.credit_card),
    Wrench(R.drawable.wrench),
    ShieldCheck(R.drawable.shield_check),
    Store(R.drawable.store),
    Tv(R.drawable.tv),
    Map(R.drawable.map),
    Briefcase(R.drawable.briefcase),
    Tag(R.drawable.tag),
    CarFront(R.drawable.car_front),
    Bike(R.drawable.bike),
    Banknote(R.drawable.banknote),
    WalletMinimal(R.drawable.wallet_minimal),
    ShoppingCart(R.drawable.shopping_cart),
    ShoppingBasket(R.drawable.shopping_basket),
    Coffee(R.drawable.coffee),
    Ticket(R.drawable.ticket),
    BoxPackage(R.drawable.box_package);

    companion object {
        /** Safe lookup by enum name (used by the icon-masker heuristics). */
        fun fromName(name: String?): LineIcon =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: AppWindow

        /**
         * P4: lookup by a **lucide kebab name** (e.g. "message-circle", "car-front"), as
         * used by the `.pen` design library. Falls back to [fromName] (the PascalCase enum
         * name) so existing callers keep working, then to [AppWindow].
         */
        fun fromLucide(kebabName: String?): LineIcon {
            if (kebabName.isNullOrBlank()) return AppWindow
            // A couple of lucide names clash with Kotlin/Java reserved words or were
            // renamed for clarity; map them explicitly (drawable `package.xml` is illegal).
            val aliased = when (kebabName.lowercase()) {
                "package" -> "BoxPackage"
                "box-package", "box_package" -> "BoxPackage"
                else -> null
            }
            if (aliased != null) return entries.firstOrNull { it.name == aliased } ?: AppWindow
            val pascal = kebabName.split('-', '_')
                .filter { it.isNotBlank() }
                .joinToString("") { part -> part.replaceFirstChar { it.uppercase() } }
            return entries.firstOrNull { it.name.equals(pascal, ignoreCase = true) }
                ?: fromName(kebabName)
        }
    }
}

@Composable
fun LineIconImage(
    icon: LineIcon,
    size: Dp,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(id = icon.resId),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}
