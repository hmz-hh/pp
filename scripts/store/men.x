#!/bin/bash

# --- منع الخروج عند الضغط على Ctrl+C وإرجاع المستخدم للقائمة الرئيسية ---
trap 'clear; echo -e "\n\e[1;33m [!] Action cancelled. Returning to menu...\e[0m"; sleep 1' SIGINT

# --- تحديد المسار المطلق التلقائي لمنع التكرار ---
# --- تحديد المسار بناءً على مسار العمل الحالي (حيت السكريبت كيتفك فـ tmp) ---
SCRIPT_DIR="$PWD"
JSON_FILE="$SCRIPT_DIR/data.json"
USERS_FILE="$SCRIPT_DIR/users_db.json"
BANNED_FILE="$SCRIPT_DIR/banned.json"

# --- Colors ---
NC='\e[0m'
YELLOW='\e[1;93m'
GREEN='\e[1;92m'
CYAN='\e[0;36m'
WHITE='\e[1;77m'
RED='\e[1;31m'
PURPLE='\e[1;35m'

# دالة العودة الافتراضية للواجهة الرئيسية الخاصة بالـ VPS
exit_to_main_panel() {
    _DIR_NAME=$(basename "$PWD")
    _BOT_KEY=${_DIR_NAME##*-}
    EXP_DATE=$(/usr/bin/curl -s "http://sc.vpsvip.site/exp" 2>/dev/null | grep -w "$_BOT_KEY" | awk '{print $2}')
    [ -z "$EXP_DATE" ] && EXP_DATE="N/A"

    clear
    echo "   -----------------------------------------------------"
    echo -e "     Status:    \e[1;32mONLINE\e[0m    exp hosting: \e[1;33m$EXP_DATE\e[0m"
    echo -e "     Enviroment: Secure Cloud Node (Isolated Environment)"
    echo "   "
    echo "     Welcome to your dedicated hosting management panel."
    echo "     All activities are securely synchronized with core APIs."
    echo "   "
    echo -e "     Type \e[1;32mmenu\e[0m to initialize user execution panel."
    echo -e "     Type \e[1;33mpasswd\e[0m to dynamically update account credentials."
    echo -e "     Type \e[1;36minfo\e[0m to manage script detailed properties."
    echo -e "     Type \e[1;35mchuser\e[0m to modify your VPS username."
    echo "   -----------------------------------------------------"
    echo "   "
    exit 0
}

# دالة الفحص الصامت لتوكن تليجرام
validate_telegram_token() {
    local token="$1"
    [ -z "$token" ] && return 1
    
    local res=$(curl -s --max-time 5 "https://api.telegram.org/bot${token}/getMe")
    local ok=$(echo "$res" | jq -r '.ok' 2>/dev/null)
    
    if [ "$ok" = "true" ]; then
        return 0
    else
        return 1
    fi
}

# دالة الفحص الصامت لـ OxaPay
validate_oxapay_key() {
    local key="$1"
    [ -z "$key" ] && return 1
    
    local res=$(curl -s -X POST https://api.oxapay.com/merchants/list -H "Content-Type: application/json" -d "{\"merchant\": \"$key\"}")
    
    if echo "$res" | grep -E -q '"result":\s*100'; then
        return 0
    else
        return 1
    fi
}

# صيانة أولية للملفات
[ ! -f "$BANNED_FILE" ] && echo "[]" > "$BANNED_FILE"

# --- 1. فحص بنية الملف بشكل آمن ---
SHOULD_SETUP=false

if [ ! -f "$JSON_FILE" ] || ! jq -e . "$JSON_FILE" >/dev/null 2>&1; then
    SHOULD_SETUP=true
else
    for key in API_TOKEN ADMIN_ID ADMIN_USERNAME BINANCE_PAY_ID BINANCE_API_KEY BINANCE_API_SECRET OXAPAY_API_KEY; do
        val=$(jq -r ".[\"$key\"]" "$JSON_FILE" 2>/dev/null)
        if [ -z "$val" ] || [ "$val" = "null" ]; then
            SHOULD_SETUP=true
            break
        fi
    done
fi

# تشغيل الإضافة (Setup Mode) فقط عند النقص الحقيقي
if [ "$SHOULD_SETUP" = true ]; then
    clear
    echo -e "${YELLOW}┌──────────────────────────────────────────────────┐${NC}"
    echo -e "${YELLOW}│${GREEN}          BOT CONFIGURATION (SETUP MODE)          ${YELLOW}│${NC}"
    echo -e "${YELLOW}└──────────────────────────────────────────────────┘${NC}"
    echo -e "Please provide the bot details (or type '${RED}exit${NC}' to return to main panel):\n"
    
    # 1. طلب التوكن
    while true; do
        read -p " 1. Enter Telegram Bot Token: " b_token
        if [ "$b_token" = "exit" ]; then exit_to_main_panel; fi
        if [ -z "$b_token" ]; then echo -e "${RED} [!] Token cannot be empty.${NC}"; continue; fi
        
        echo -e "${YELLOW} Checking token validity...${NC}"
        if validate_telegram_token "$b_token"; then
            echo -e "${GREEN} [✓] Token verified successfully!${NC}"
            break
        else
            echo -e "${RED} [X] Token verification failed (or network timeout).${NC}"
            read -p " Do you want to use this token anyway? (y/n): " choice
            if [[ "$choice" =~ ^[Yy]$ ]]; then
                break
            else
                continue
            fi
        fi
    done

    # 2. طلب آيدي الأدمن
    while true; do
        read -p " 2. Enter Admin Telegram ID: " b_id
        if [ "$b_id" = "exit" ]; then exit_to_main_panel; fi
        if [[ "$b_id" =~ ^[0-9]+$ ]]; then
            break
        else
            echo -e "${RED} [!] Admin ID must be a valid number.${NC}"
            continue
        fi
    done

    # طلب باقي البيانات
    read -p " 3. Enter Admin Username: " b_user
    [ "$b_user" = "exit" ] && exit_to_main_panel

    read -p " 4. Enter Binance Pay ID: " b_pay
    [ "$b_pay" = "exit" ] && exit_to_main_panel

    read -p " 5. Enter Binance API Key: " b_api
    [ "$b_api" = "exit" ] && exit_to_main_panel

    read -p " 6. Enter Binance API Secret: " b_sec
    [ "$b_sec" = "exit" ] && exit_to_main_panel

    # 7. طلب OxaPay مع إمكانية التخطي
    while true; do
        read -p " 7. Enter OxaPay API Key (or type 'skip' to disable): " o_key
        if [ "$o_key" = "exit" ]; then exit_to_main_panel; fi
        
        if [[ -z "$o_key" || "$o_key" == "skip" ]]; then
            echo -e "${YELLOW} [!] Skipped. OxaPay disabled.${NC}"
            o_key=""
            o_status="error"
            break
        fi

        echo -e "${YELLOW} Checking OxaPay key validity...${NC}"
        if validate_oxapay_key "$o_key"; then
            echo -e "${GREEN} [✓] Valid Key! Saved.${NC}"
            o_status="ok"
            break
        else
            echo -e "${RED} [X] Invalid Key. Try again.${NC}"
        fi
    done

    # حفظ البيانات
    jq -n \
      --arg token "$b_token" \
      --argjson id "$b_id" \
      --arg user "$b_user" \
      --arg pay "$b_pay" \
      --arg api "$b_api" \
      --arg sec "$b_sec" \
      --arg oxa "$o_key" \
      --arg o_stat "$o_status" \
      '{API_TOKEN: $token, ADMIN_ID: $id, ADMIN_USERNAME: $user, BINANCE_PAY_ID: $pay, BINANCE_API_KEY: $api, BINANCE_API_SECRET: $sec, ADMIN_SECRET: $sec, OXAPAY_API_KEY: $oxa, oxapay_status: $o_stat}' > "$JSON_FILE"

    chmod 666 "$JSON_FILE"
    echo -e "${GREEN}\n [SUCCESS] All settings saved permanently! Starting bot...${NC}"
    sleep 1.5
fi

# التشغيل التلقائي للبوت
restart-bot > /dev/null 2>&1

# --- 2. Functions for Menu Actions ---
ban_user() {
    echo -ne "\n${CYAN} Enter Username to Ban: ${NC}"
    read b_user
    if [ -n "$b_user" ]; then
        clean_user=$(echo "$b_user" | sed 's/@//')
        jq --arg u "$clean_user" '. += [$u] | unique' "$BANNED_FILE" > "$SCRIPT_DIR/temp.json" && mv "$SCRIPT_DIR/temp.json" "$BANNED_FILE"
        echo -e "${GREEN} [DONE] User @$clean_user Banned!${NC}"
    fi
    sleep 2
}

unban_user() {
    echo -e "\n${RED} ┌───────── CURRENT BANNED USERS ──────────┐${NC}"
    local count=$(jq '. | length' "$BANNED_FILE" 2>/dev/null)
    if [ "$count" -eq 0 ] 2>/dev/null; then
        echo -e "${YELLOW}          No banned users found.         ${NC}"
    else
        jq -r '.[]' "$BANNED_FILE" | sed 's/^/  @/'
    fi
    echo -e "${RED} └─────────────────────────────────────────┘${NC}"

    echo -ne "\n${CYAN} Enter Username to Unban: ${NC}"
    read u_user
    if [ -n "$u_user" ]; then
        clean_user=$(echo "$u_user" | sed 's/@//')
        jq --arg u "$clean_user" 'del(.[] | select(. == $u))' "$BANNED_FILE" > "$SCRIPT_DIR/temp.json" && mv "$SCRIPT_DIR/temp.json" "$BANNED_FILE"
        echo -e "${GREEN} [DONE] User @$clean_user Unbanned!${NC}"
    fi
    sleep 2
}

restart_bot() {
    echo -e "\n${YELLOW} [WAIT] Restarting Bot...${NC}"
    restart-bot > /dev/null 2>&1
    echo -e "${GREEN} [SUCCESS] Bot restarted!${NC}"
    sleep 2
}

# دالة التعديل الآمنة كلياً
edit_value() {
    local key=$1
    local label=$2
    local current_val=$(jq -r ".[\"$key\"] // \"N/A\"" "$JSON_FILE" 2>/dev/null)
    
    echo -e "\n${CYAN} Editing: ${WHITE}$label${NC}"
    echo -e "${YELLOW} Current Value:${NC} $current_val"
    
    while true; do
        read -p " Enter new value (or type 'exit' to return to main panel): " n_val
        if [ "$n_val" = "exit" ]; then
            exit_to_main_panel
        fi
        
        # التعامل مع الإدخال الفارغ أو أمر التخطي (مخصص لـ OxaPay)
        if [ -z "$n_val" ]; then
            if [[ "$key" == "OXAPAY_API_KEY" ]]; then
                n_val="skip"
            else
                echo -e "${RED} [SKIP] No value entered.${NC}"
                sleep 1
                return
            fi
        fi

        # فحوصات مخصصة لكل نوع
        if [[ "$key" == "API_TOKEN" ]]; then
            echo -e "${YELLOW} Checking token validity...${NC}"
            if ! validate_telegram_token "$n_val"; then
                echo -e "${RED} [X] Token validation failed.${NC}"
                read -p " Do you want to force save this token anyway? (y/n): " force_choice
                if [[ ! "$force_choice" =~ ^[Yy]$ ]]; then
                    continue
                fi
            fi
        elif [[ "$key" == "OXAPAY_API_KEY" ]]; then
            if [[ "$n_val" == "skip" ]]; then
                echo -e "${YELLOW} [!] Skipped. OxaPay disabled.${NC}"
                jq '.OXAPAY_API_KEY = "" | .oxapay_status = "error"' "$JSON_FILE" > "$SCRIPT_DIR/temp.json" && mv "$SCRIPT_DIR/temp.json" "$JSON_FILE"
                break
            fi
            
            echo -e "${YELLOW} Checking OxaPay key validity...${NC}"
            if validate_oxapay_key "$n_val"; then
                echo -e "${GREEN} [✓] Valid Key! Saved.${NC}"
                jq --arg v "$n_val" '.OXAPAY_API_KEY = $v | .oxapay_status = "ok"' "$JSON_FILE" > "$SCRIPT_DIR/temp.json" && mv "$SCRIPT_DIR/temp.json" "$JSON_FILE"
                break
            else
                echo -e "${RED} [X] Invalid Key. Try again (or type 'skip' to disable).${NC}"
                continue
            fi
        fi

        # الحفظ لجميع القيم الأخرى
        if [[ "$key" != "OXAPAY_API_KEY" ]]; then
            if [[ "$key" == "ADMIN_ID" ]]; then
                if [[ "$n_val" =~ ^[0-9]+$ ]]; then
                    jq --argjson v "$n_val" --arg k "$key" '.[$k] = $v' "$JSON_FILE" > "$SCRIPT_DIR/temp.json" && mv "$SCRIPT_DIR/temp.json" "$JSON_FILE"
                    break
                else
                    echo -e "${RED} [ERROR] Admin ID must be a number!${NC}"
                    continue
                fi
            else
                jq --arg v "$n_val" --arg k "$key" '.[$k] = $v' "$JSON_FILE" > "$SCRIPT_DIR/temp.json" && mv "$SCRIPT_DIR/temp.json" "$JSON_FILE"
                
                if [[ "$key" == "BINANCE_API_SECRET" ]]; then
                    jq --arg v "$n_val" '.ADMIN_SECRET = $v' "$JSON_FILE" > "$SCRIPT_DIR/temp.json" && mv "$SCRIPT_DIR/temp.json" "$JSON_FILE"
                fi
                break
            fi
        fi
    done
    echo -e "${GREEN} [DONE] Updated safely!${NC}"
    restart-bot > /dev/null 2>&1
    sleep 1
}

# --- 3. Main Interface Loop ---
while true; do
    clear
    echo -e "${YELLOW} ┌──────────────────────────────────┐${NC}"
    echo -e "${YELLOW}${GREEN}    .::. ${WHITE}BOT ADVANCED MANAGER ${GREEN}.::. ${YELLOW}${NC}"
    echo -e "${YELLOW} └──────────────────────────────────┘${NC}"
    echo -e "${YELLOW} ┌──────────────────────────────────┐${NC}"
    echo -e "${YELLOW} │  ${GREEN}1.${NC} ${CYAN}Change Telegram Bot Token${NC}"
    echo -e "${YELLOW} │  ${GREEN}2.${NC} ${CYAN}Change Admin Telegram ID${NC}"
    echo -e "${YELLOW} │  ${GREEN}3.${NC} ${CYAN}Change Admin Username${NC}"
    echo -e "${YELLOW} │  ${GREEN}4.${NC} ${CYAN}Change Binance Pay ID${NC}"
    echo -e "${YELLOW} │  ${GREEN}5.${NC} ${CYAN}Change Binance API Key${NC}"
    echo -e "${YELLOW} │  ${GREEN}6.${NC} ${CYAN}Change Binance API Secret${NC}"
    echo -e "${YELLOW} │  ${GREEN}7.${NC} ${CYAN}Change OxaPay API Key${NC}"
    echo -e "${YELLOW} │  ${GREEN}8.${NC} ${CYAN}Ban a User (Username)${NC}"
    echo -e "${YELLOW} │  ${GREEN}9.${NC} ${CYAN}Unban a User (Username)${NC}"
    echo -e "${YELLOW} │  ${GREEN}10.${NC} ${CYAN}Restart Bot (PM2)${NC}"
    echo -e "${YELLOW} │  ${GREEN}x.${NC} ${CYAN}Exit Manager${NC}"
    echo -e "${YELLOW} └──────────────────────────────────┘${NC}"
    echo -e ""
    read -p " Select From Options [ 1 - 10 ] : " menu

    case $menu in
        1) edit_value "API_TOKEN" "Telegram Bot Token" ;;
        2) edit_value "ADMIN_ID" "Admin Telegram ID" ;;
        3) edit_value "ADMIN_USERNAME" "Admin Username" ;;
        4) edit_value "BINANCE_PAY_ID" "Binance Pay ID" ;;
        5) edit_value "BINANCE_API_KEY" "Binance API Key" ;;
        6) edit_value "BINANCE_API_SECRET" "Binance API Secret" ;;
        7) edit_value "OXAPAY_API_KEY" "OxaPay API Key (or 'skip' to disable)" ;;
        8) ban_user ;;
        9) unban_user ;;
        10) restart_bot ;;
        x|X) exit_to_main_panel ;;
        *) echo -e "${RED} Invalid Option!${NC}"; sleep 1 ;;
    esac
done
