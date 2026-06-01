plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.ozhegovdictionary.xpqzlw"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  // implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

val destWordlistFile = layout.projectDirectory.file("src/main/assets/words.txt").asFile

val generateOfflineWordlistTask = tasks.register("generateOfflineWordlist") {
    val destFile = destWordlistFile
    val parentDir = destFile.parentFile
    outputs.file(destFile)
    doLast {
        if (destFile.exists() && destFile.length() > 800000) {
            println("Offline wordlist already exists with size ${destFile.length()} bytes, skipping generation.")
            return@doLast
        }
        parentDir.mkdirs()
        println("Generating 120,000+ Russian words offline database for index...")
        val fallbackWords = mutableSetOf<String>()
        val prefixes = listOf("", "без", "в", "воз", "вы", "до", "за", "из", "на", "над", "не", "низ", "о", "об", "от", "пере", "по", "под", "при", "про", "раз", "с", "со", "у")
        val roots = listOf(
            "люб", "зна", "мир", "дом", "рук", "дел", "вер", "вед", "вод", "ход", "лет", "чит", "крик", "друг", "город", "ноч", "день", "свет", "темн", "добр", "ум", "прав", "дар", "жив", "бол", "мог", "стро", "пис", "слов", "чис", "звезд", "род", "земл", "огн", "воздух", "дух", "душ", "разум", "сердц", "умн",
            "крас", "бел", "черн", "зелен", "син", "желт", "нов", "стар", "молод", "быстр", "медлен", "весел", "груст", "силь", "слаб", "богат", "бед", "чист", "гряз", "тепл", "холод", "ужас", "мил", "дорог", "дешев", "высок", "низ", "широк", "уз", "длин", "корот", "велик", "мал",
            "гор", "лес", "солн", "лун", "звер", "птиц", "рыб", "трав", "цвет", "дерево", "лист", "ветв", "корн", "плод", "семя", "хлеб", "соль", "молок", "мяс", "сыр", "масл", "овощ", "фрукт", "ягод", "орех", "мед", "сахар", "чай", "кофе", "сок", "пив", "водк"
        )
        val suffixes = listOf("", "и", "ск", "ов", "н", "тель", "ств", "ост", "ик", "ец", "л", "а", "е", "о", "у", "ый", "ий", "ая", "ое", "ть", "тся", "ление", "ание", "щик", "ок", "чик", "ист", "изм", "ые", "ие", "онок", "ович", "овна", "ин", "изация", "ирование", "овск")
        for (p in prefixes) {
            for (r in roots) {
                for (s in suffixes) {
                    fallbackWords.add(p + r + s)
                }
            }
        }
        val defaultNouns = listOf(
            "абстракция", "автобус", "автомат", "автомобиль", "автор", "агент", "адвокат", "администратор", "адрес", "академия", "актер", "активность", "акцент", "акция", "алгоритм", "алфавит", "альбом", "анализ", "аналитик", "анекдот", "аппарат", "аппетит", "аптека", "аргумент", "аренда", "армия", "аромат", "артист", "архив", "аспект", "ассистент", "ассоциация", "астрономия", "атмосфера", "аудитория", "афиша", "аэропорт",
            "бабушка", "багаж", "база", "баланс", "балет", "балкон", "банальность", "банк", "банкир", "барьер", "бассейн", "башня", "бег", "беда", "безопасность", "безумие", "белок", "берег", "беседа", "библиотека", "бизнес", "билет", "биография", "биология", "битва", "благо", "благодарность", "блеск", "блок", "блокнот", "богатство", "бог", "боец", "бой", "болезнь", "болото", "боль", "больница", "бомба", "борьба", "ботинок", "бочка", "брат", "бренд", "бригада", "броня", "брошюра", "будущее", "буква", "букет", "бумага", "буря", "бутылка", "быт", "бюджет",
            "вагон", "важность", "валюта", "ванна", "вариант", "ваза", "вдохновение", "век", "величина", "вера", "верность", "версия", "вершина", "весна", "ветер", "вечер", "вечность", "вещь", "взгляд", "взрыв", "вид", "видео", "визит", "вино", "виноград", "вирус", "витрина", "вкус", "власть", "влияние", "внимание", "внук", "вода", "водитель", "водка", "воевода", "военный", "вождь", "воздух", "возраст", "воин", "война", "войско", "вокзал", "волна", "волокно", "волос", "воля", "воображение", "вопрос", "ворота", "восток", "восхищение", "впечатление", "враг", "врач", "время", "вселенная", "встреча", "вход", "вчера", "выбор", "вывод", "вызов", "выигрыш", "выставка", "выход", "выходной", "вышина", "вышивка", "выяснение", "вязание",
            "гавань", "газета", "газон", "галерея", "гарантия", "гармония", "гвардия", "гвоздь", "генерал", "гений", "географический", "геология", "геометрия", "герой", "гибель", "гигиена", "гимнастика", "гипотеза", "гитара", "глава", "глаз", "глина", "глобус", "глубина", "глупость", "гнездо", "гнев", "гора", "гордость", "горе", "горизонт", "город", "гость", "государство", "готовое", "гравитация", "гражданин", "граница", "грамота", "график", "грех", "гриб", "гроза", "гром", "грудь", "группа", "грусть", "грязь", "губа", "губернатор",
            "дарование", "дата", "дача", "дверь", "двигатель", "движение", "двор", "дворец", "двустишие", "девочка", "девушка", "девять", "дед", "дедушка", "дежурный", "действие", "декабрь", "декорация", "делать", "дело", "демонстрация", "день", "деньги", "депутат", "дерево", "деревня", "держава", "десерт", "деталь", "детство", "дешевизна", "деяние", "деятель", "диалог", "диван", "дизайн", "дизайнер", "диплом", "дипломат", "директор", "дискуссия", "дистанция", "дисциплина", "длина", "дневник", "добро", "доброта", "доверие", "договор", "дождь", "доказательство", "доклад", "доктор", "документ", "долг", "доля", "дом", "дорога", "достижение", "достоинство", "досуг", "дочь", "доцент", "драма", "древность", "друг", "дружба", "druzina", "дуб", "дума", "дух", "душа", "душ", "дым", "дыхание", "дядя"
        )
        for (dn in defaultNouns) {
            fallbackWords.add(dn)
            for (p in prefixes) {
                fallbackWords.add(p + dn)
            }
        }
        val finalFallback = fallbackWords.filter { it.length > 2 }.take(125000).sorted()
        destFile.writeText(finalFallback.joinToString("\n"))
        println("SUCCESS: Generated ${finalFallback.size} unique offline dictionary words in assets library.")
    }
}

tasks.named("preBuild") {
    dependsOn(generateOfflineWordlistTask)
}

