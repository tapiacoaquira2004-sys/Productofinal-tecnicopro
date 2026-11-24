package com.miscursos.tecnicopro

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.checkSelfPermission
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

// --- PALETA DE COLORES ---
val AzulMarino = Color(0xFF13344C)
val Turquesa = Color(0xFF387D8A)
val NaranjaFirma = Color(0xFFEE8133)
val FondoGrisClaro = Color(0xFFF2F4F7)
val EstrellaDorada = Color(0xFFF5A623)

data class Tecnico(
    val id: String,
    val nombre: String,
    val oficio: String,
    val categoria: String,
    val calificacion: String,
    val resenas: Int,
    val registro: String,
    val descripcion: String,
    val costoEstimado: String,
    val sedeDireccion: String,
    val ubicacionSede: GeoPoint
)

data class ContratoRemoto(
    val id: Int,
    val cliente: String,
    val celular: String,
    val correo: String,
    val ubicacion: String,
    val referencia: String,
    val problema: String,
    val tecnico: String,
    val costo: String,
    val estado: String,
    val fecha: String
)

val listaTecnicos = listOf(
    Tecnico(
        id = "SA-SM456",
        nombre = "Mateo Lopez",
        oficio = "Gasfitero Certificado",
        categoria = "Gasfitería",
        calificacion = "4.9",
        resenas = 112,
        registro = "Reg. de Gasfitería #1234 • Seguridad OSHA",
        descripcion = "Reparación excelente de tubería residencial, instalación de termas y mantenimiento preventivo.",
        costoEstimado = "S/. 1,200.00",
        sedeDireccion = "Sede Central TécnicoPro - Plaza Principal, Tarata",
        ubicacionSede = GeoPoint(-17.4745, -70.0328)
    ),
    Tecnico(
        id = "SA-RB789",
        nombre = "Robert Banier",
        oficio = "Electricista Certificado",
        categoria = "Electricidad",
        calificacion = "4.9",
        resenas = 98,
        registro = "Licencia Electrosur • Tableros Industriales",
        descripcion = "Instalaciones eléctricas domiciliarias, cableado estructurado y pozos a tierra.",
        costoEstimado = "S/. 950.00",
        sedeDireccion = "Taller TécnicoPro Norte - Av. Principal, Tarata",
        ubicacionSede = GeoPoint(-17.4762, -70.0345)
    ),
    Tecnico(
        id = "SA-MD321",
        nombre = "Marin Davis",
        oficio = "Especialista HVAC",
        categoria = "HVAC",
        calificacion = "4.8",
        resenas = 74,
        registro = "Certificación Climatización y Refrigeración",
        descripcion = "Mantenimiento y reparación de equipos de aire acondicionado y calefacción.",
        costoEstimado = "S/. 800.00",
        sedeDireccion = "Sede Central TécnicoPro - Plaza Principal, Tarata",
        ubicacionSede = GeoPoint(-17.4745, -70.0328)
    ),
    Tecnico(
        id = "SA-SR654",
        nombre = "Sofia Ruiz",
        oficio = "Carpintera Ebanista",
        categoria = "Carpintería",
        calificacion = "4.9",
        resenas = 130,
        registro = "Diseño de Mobiliario y Estructuras en Madera",
        descripcion = "Fabricación e instalación de puertas, reposteros y acabados finos en madera.",
        costoEstimado = "S/. 1,100.00",
        sedeDireccion = "Taller Madera TécnicoPro - Tarata",
        ubicacionSede = GeoPoint(-17.4730, -70.0315)
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = "TecnicoProApp/1.0 (Android)"
        val basePath = File(cacheDir.absolutePath, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        osmConfig.osmdroidTileCache = File(basePath, "tiles")

        setContent {
            MaterialTheme {
                AppTecnicoPro()
            }
        }
    }
}

@Composable
fun AppTecnicoPro() {
    var pantallaActual by remember { mutableStateOf("directorio") }
    var tecnicoSeleccionado by remember { mutableStateOf(listaTecnicos[0]) }

    var nombreCliente by remember { mutableStateOf("Daniel Tapia") }
    var celularCliente by remember { mutableStateOf("987654321") }
    var correoCliente by remember { mutableStateOf("daniel@email.com") }
    var ubicacionCliente by remember { mutableStateOf("Av. Principal 123, Tarata") }
    var referenciaCliente by remember { mutableStateOf("") }
    var problemaCliente by remember { mutableStateOf("Fuga de agua en la tubería principal del baño.") }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = pantallaActual == "directorio",
                    onClick = { pantallaActual = "directorio" },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Inicio", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Turquesa, selectedTextColor = Turquesa)
                )
                NavigationBarItem(
                    selected = pantallaActual == "mapa",
                    onClick = { pantallaActual = "mapa" },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    label = { Text("Ubicación", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Turquesa, selectedTextColor = Turquesa)
                )
                NavigationBarItem(
                    selected = pantallaActual == "contrato",
                    onClick = { pantallaActual = "contrato" },
                    icon = { Icon(Icons.Default.Description, contentDescription = null) },
                    label = { Text("Acuerdo", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Turquesa, selectedTextColor = Turquesa)
                )
                NavigationBarItem(
                    selected = pantallaActual == "seguimiento",
                    onClick = { pantallaActual = "seguimiento" },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                    label = { Text("Contratos", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Turquesa, selectedTextColor = Turquesa)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(FondoGrisClaro)
        ) {
            when (pantallaActual) {
                "directorio" -> PantallaDirectorio(
                    onVerUbicacion = { tecnico ->
                        tecnicoSeleccionado = tecnico
                        pantallaActual = "mapa"
                    },
                    onContratar = { tecnico ->
                        tecnicoSeleccionado = tecnico
                        pantallaActual = "contrato"
                    }
                )
                "contrato" -> PantallaAcuerdoServicio(
                    tecnico = tecnicoSeleccionado,
                    nombreCliente = nombreCliente,
                    onCambioNombre = { nombreCliente = it },
                    celularCliente = celularCliente,
                    onCambioCelular = { celularCliente = it },
                    correoCliente = correoCliente,
                    onCambioCorreo = { correoCliente = it },
                    ubicacionCliente = ubicacionCliente,
                    onCambioUbicacion = { ubicacionCliente = it },
                    referenciaCliente = referenciaCliente,
                    onCambioReferencia = { referenciaCliente = it },
                    problemaCliente = problemaCliente,
                    onCambioProblema = { problemaCliente = it },
                    onVolver = { pantallaActual = "directorio" },
                    onFirmarExito = { pantallaActual = "seguimiento" }
                )
                "seguimiento" -> PantallaSeguimientoContratos(
                    tecnico = tecnicoSeleccionado,
                    onVolver = { pantallaActual = "directorio" },
                    onVerMapa = { pantallaActual = "mapa" }
                )
                "mapa" -> PantallaUbicacionEmpresa(
                    tecnico = tecnicoSeleccionado,
                    onVolver = { pantallaActual = "directorio" }
                )
            }
        }
    }
}

// =========================================================================
// PANTALLA 1: DIRECTORIO
// =========================================================================
@Composable
fun PantallaDirectorio(
    onVerUbicacion: (Tecnico) -> Unit,
    onContratar: (Tecnico) -> Unit
) {
    var textoBusqueda by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("Todos") }

    val categorias = listOf(
        Pair("Todos", Icons.Default.Apps),
        Pair("Gasfitería", Icons.Default.Plumbing),
        Pair("Electricidad", Icons.Default.ElectricalServices),
        Pair("HVAC", Icons.Default.AcUnit),
        Pair("Carpintería", Icons.Default.Handyman)
    )

    val tecnicosFiltrados = listaTecnicos.filter {
        (categoriaSeleccionada == "Todos" || it.categoria == categoriaSeleccionada) &&
                (it.nombre.contains(textoBusqueda, ignoreCase = true) ||
                        it.oficio.contains(textoBusqueda, ignoreCase = true))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AzulMarino)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("TécnicoPro Directorio", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Surface(shape = CircleShape, color = Turquesa, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.padding(6.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = textoBusqueda,
                onValueChange = { textoBusqueda = it },
                placeholder = { Text("Buscar por Profesión o Nombre", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            categorias.forEach { (nombre, icono) ->
                val seleccionado = categoriaSeleccionada == nombre
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (seleccionado) Turquesa else Color.Transparent)
                        .clickable { categoriaSeleccionada = nombre }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(icono, contentDescription = null, tint = if (seleccionado) Color.White else Color.DarkGray, modifier = Modifier.size(22.dp))
                    Text(nombre, fontSize = 12.sp, color = if (seleccionado) Color.White else Color.DarkGray, fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tecnicosFiltrados) { tecnico ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = AzulMarino.copy(alpha = 0.12f), modifier = Modifier.size(52.dp)) {
                                Icon(Icons.Default.Engineering, contentDescription = null, tint = AzulMarino, modifier = Modifier.padding(10.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tecnico.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AzulMarino)
                                Text(tecnico.oficio, fontSize = 13.sp, color = Color.DarkGray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = EstrellaDorada, modifier = Modifier.size(16.dp))
                                    Text(" ${tecnico.calificacion} (${tecnico.resenas} reseñas)", fontSize = 12.sp, color = Color.Gray)
                                }
                                Text(tecnico.registro, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(tecnico.descripcion, fontSize = 12.sp, color = Color.DarkGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { onVerUbicacion(tecnico) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Turquesa)
                                Spacer(Modifier.width(4.dp))
                                Text("Ver Ubicación", fontSize = 12.sp, color = AzulMarino)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { onContratar(tecnico) },
                                colors = ButtonDefaults.buttonColors(containerColor = Turquesa),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text("Contratar", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// PANTALLA 2: ACUERDO DE SERVICIO (CON REFERENCIA O GPS)
// =========================================================================
@SuppressLint("MissingPermission")
@Composable
fun PantallaAcuerdoServicio(
    tecnico: Tecnico,
    nombreCliente: String,
    onCambioNombre: (String) -> Unit,
    celularCliente: String,
    onCambioCelular: (String) -> Unit,
    correoCliente: String,
    onCambioCorreo: (String) -> Unit,
    ubicacionCliente: String,
    onCambioUbicacion: (String) -> Unit,
    referenciaCliente: String,
    onCambioReferencia: (String) -> Unit,
    problemaCliente: String,
    onCambioProblema: (String) -> Unit,
    onVolver: () -> Unit,
    onFirmarExito: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var cargando by remember { mutableStateOf(false) }

    fun capturarGPS() {
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    val coords = String.format(Locale.US, "GPS %.5f, %.5f", loc.latitude, loc.longitude)
                    val nuevaRef = if (referenciaCliente.isBlank()) coords else "$referenciaCliente ($coords)"
                    onCambioReferencia(nuevaRef)
                    Toast.makeText(context, "Coordenadas GPS agregadas", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Activa el GPS para obtener tu ubicación", Toast.LENGTH_SHORT).show()
                }
            }
    }

    val gpsLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) capturarGPS() else Toast.makeText(context, "Permiso GPS denegado", Toast.LENGTH_SHORT).show()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(AzulMarino)
                .padding(12.dp)
        ) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text("Acuerdo de Servicio", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Acuerdo con: ${tecnico.nombre}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AzulMarino)
                    Text("ID Contrato: ${tecnico.id}  •  Especialidad: ${tecnico.categoria}", fontSize = 12.sp, color = Color.Gray)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text("TÉRMINOS Y CONDICIONES", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AzulMarino)
                    Text("Este acuerdo describe los términos de pago, confidencialidad y garantías del servicio técnico.", fontSize = 12.sp, color = Color.DarkGray)

                    Spacer(Modifier.height(10.dp))
                    Text("ALCANCE DEL TRABAJO", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AzulMarino)
                    Text(tecnico.descripcion, fontSize = 12.sp, color = Color.DarkGray)

                    Spacer(Modifier.height(10.dp))
                    Text("SECCIÓN DE COSTOS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AzulMarino)
                    Text("Total Estimado: ${tecnico.costoEstimado} (Adelanto del 50% al firmar).", fontSize = 12.sp, color = Color.DarkGray)

                    Spacer(Modifier.height(14.dp))
                    Text("DATOS Y REQUERIMIENTO DEL CLIENTE", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AzulMarino)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nombreCliente,
                        onValueChange = onCambioNombre,
                        label = { Text("Nombre Completo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = celularCliente,
                        onValueChange = onCambioCelular,
                        label = { Text("Número de Celular") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = correoCliente,
                        onValueChange = onCambioCorreo,
                        label = { Text("Correo Electrónico") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = ubicacionCliente,
                        onValueChange = onCambioUbicacion,
                        label = { Text("Ubicación / Dirección de la Casa") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = referenciaCliente,
                        onValueChange = onCambioReferencia,
                        label = { Text("Referencia o Coordenadas GPS") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = {
                            if (checkSelfPermission(context, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
                                capturarGPS()
                            } else {
                                gpsLauncher.launch(ACCESS_FINE_LOCATION)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = Turquesa, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Usar mi ubicación GPS actual", color = AzulMarino, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = problemaCliente,
                        onValueChange = onCambioProblema,
                        label = { Text("Descripción breve del problema") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (
                                nombreCliente.isBlank() ||
                                celularCliente.isBlank() ||
                                ubicacionCliente.isBlank() ||
                                problemaCliente.isBlank()
                            ) {
                                Toast.makeText(
                                    context,
                                    "Completa nombre, celular, ubicación y problema.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            cargando = true
                            scope.launch {
                                val resultado = enviarSolicitudAXAMPP(
                                    cliente = nombreCliente.trim(),
                                    celular = celularCliente.trim(),
                                    correo = correoCliente.trim(),
                                    ubicacion = ubicacionCliente.trim(),
                                    referencia = referenciaCliente.trim(),
                                    problema = problemaCliente.trim(),
                                    tecnico = "${tecnico.nombre} (${tecnico.oficio})",
                                    costo = tecnico.costoEstimado
                                )
                                cargando = false
                                if (resultado == "OK") {
                                    Toast.makeText(context, "¡Contrato enviado a la central!", Toast.LENGTH_SHORT).show()
                                    onFirmarExito()
                                } else {
                                    Toast.makeText(context, resultado, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !cargando,
                        colors = ButtonDefaults.buttonColors(containerColor = NaranjaFirma),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (cargando) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Firmar Contrato y Enviar a Coordinación", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

suspend fun enviarSolicitudAXAMPP(
    cliente: String,
    celular: String,
    correo: String,
    ubicacion: String,
    referencia: String,
    problema: String,
    tecnico: String,
    costo: String
): String =
    withContext(Dispatchers.IO) {
        try {
            val url = URL("http://192.168.1.16/tecnicpro/guardar_solicitud.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connectTimeout = 8000
                readTimeout = 8000
            }

            val parametros = "cliente=${URLEncoder.encode(cliente, "UTF-8")}" +
                    "&celular=${URLEncoder.encode(celular, "UTF-8")}" +
                    "&correo=${URLEncoder.encode(correo, "UTF-8")}" +
                    "&ubicacion=${URLEncoder.encode(ubicacion, "UTF-8")}" +
                    "&referencia=${URLEncoder.encode(referencia, "UTF-8")}" +
                    "&problema=${URLEncoder.encode(problema, "UTF-8")}" +
                    "&tecnico=${URLEncoder.encode(tecnico, "UTF-8")}" +
                    "&costo=${URLEncoder.encode(costo, "UTF-8")}"

            conn.outputStream.write(parametros.toByteArray(Charsets.UTF_8))

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val respuesta = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                if (respuesta == "SUCCESS") "OK" else "PHP Error: $respuesta"
            } else {
                "HTTP Error: $responseCode"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Excepción: ${e.localizedMessage}"
        }
    }

// =========================================================================
// PANTALLA 3: SEGUIMIENTO DE CONTRATOS (DATOS REALES DESDE XAMPP)
// =========================================================================
suspend fun obtenerContratosDesdeXAMPP(): Result<List<ContratoRemoto>> =
    withContext(Dispatchers.IO) {
        try {
            val url = URL("http://192.168.1.16/tecnicpro/obtener_contratos.php")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val cuerpo = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(cuerpo)
                val lista = mutableListOf<ContratoRemoto>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    lista.add(
                        ContratoRemoto(
                            id = obj.optInt("id"),
                            cliente = obj.optString("cliente"),
                            celular = obj.optString("celular"),
                            correo = obj.optString("correo"),
                            ubicacion = obj.optString("ubicacion"),
                            referencia = obj.optString("referencia"),
                            problema = obj.optString("problema"),
                            tecnico = obj.optString("tecnico"),
                            costo = obj.optString("costo"),
                            estado = obj.optString("estado", "Pendiente"),
                            fecha = obj.optString("fecha")
                        )
                    )
                }
                Result.success(lista)
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

private fun porcentajePorEstado(estado: String): Float = when (estado) {
    "Pendiente" -> 0.20f
    "Asignado" -> 0.45f
    "En proceso" -> 0.75f
    "Finalizado" -> 1.0f
    else -> 0.25f
}

@Composable
fun PantallaSeguimientoContratos(
    tecnico: Tecnico,
    onVolver: () -> Unit,
    onVerMapa: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var contratos by remember { mutableStateOf<List<ContratoRemoto>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    fun recargar() {
        cargando = true
        mensajeError = null
        scope.launch {
            val res = obtenerContratosDesdeXAMPP()
            cargando = false
            res.onSuccess { contratos = it }
                .onFailure { mensajeError = "No se pudo cargar desde XAMPP: ${it.localizedMessage}" }
        }
    }

    LaunchedEffect(Unit) {
        recargar()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(AzulMarino)
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text("Seguimiento de Contratos", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { recargar() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onVerMapa,
                colors = ButtonDefaults.buttonColors(containerColor = Turquesa),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Directions, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Cómo llegar a la Sede (${tecnico.nombre})", color = Color.White)
            }

            if (cargando) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Turquesa)
                }
            } else if (mensajeError != null) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                    Text(mensajeError ?: "", color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                }
            } else if (contratos.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                    Text("Aún no hay contratos registrados en MySQL.", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                }
            } else {
                contratos.forEach { c ->
                    val avance = porcentajePorEstado(c.estado)
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Contrato #${c.id} • ${c.estado}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AzulMarino)
                                Text(c.costo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Turquesa)
                            }
                            Text("Cliente: ${c.cliente} (${c.celular})", fontSize = 12.sp, color = Color.DarkGray)
                            Text("Técnico: ${c.tecnico}", fontSize = 12.sp, color = Color.DarkGray)
                            Text("Ubicación: ${c.ubicacion}", fontSize = 12.sp, color = Color.Gray)
                            if (c.referencia.isNotBlank()) {
                                Text("Referencia / GPS: ${c.referencia}", fontSize = 12.sp, color = Turquesa)
                            }
                            if (c.problema.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("Problema: ${c.problema}", fontSize = 12.sp, color = Color.DarkGray)
                            }

                            Spacer(Modifier.height(10.dp))
                            Text("Avance según estado (${(avance * 100).toInt()}%)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AzulMarino)
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { avance },
                                color = Turquesa,
                                trackColor = Color.LightGray.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape)
                            )
                            if (c.fecha.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text("Registrado: ${c.fecha}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// PANTALLA 4: UBICACIÓN DE LA EMPRESA (MAPA)
// =========================================================================
@Composable
fun PantallaUbicacionEmpresa(
    tecnico: Tecnico,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val destinoEmpresa = tecnico.ubicacionSede
    var estadoRuta by remember { mutableStateOf("Sede: ${tecnico.sedeDireccion}") }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    fun dibujarRutaHaciaEmpresa(miUbicacion: GeoPoint, puntosRuta: List<GeoPoint>) {
        mapView?.apply {
            overlays.clear()
            overlays.add(Polyline().apply {
                setPoints(puntosRuta)
                outlinePaint.color = AndroidColor.parseColor("#1A73E8")
                outlinePaint.strokeWidth = 14f
            })
            overlays.add(Marker(this).apply {
                position = miUbicacion
                title = "Tu ubicación actual"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            })
            overlays.add(Marker(this).apply {
                position = destinoEmpresa
                title = "Empresa TécnicoPro"
                snippet = tecnico.sedeDireccion
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                showInfoWindow()
            })
            zoomToBoundingBox(BoundingBox.fromGeoPoints(listOf(miUbicacion, destinoEmpresa) + puntosRuta), true, 120)
            invalidate()
        }
    }

    @SuppressLint("MissingPermission")
    fun calcularRutaDesdeMiGPS() {
        if (checkSelfPermission(context, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) return

        estadoRuta = "Obteniendo GPS y trazando ruta..."
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc == null) {
                    estadoRuta = "Activa el GPS de tu celular."
                    return@addOnSuccessListener
                }
                val miPunto = GeoPoint(loc.latitude, loc.longitude)
                scope.launch {
                    try {
                        val ruta = obtenerRutaOSRM(miPunto, destinoEmpresa)
                        if (ruta != null) {
                            val (puntos, km, min) = ruta
                            estadoRuta = "Distancia: ${"%.1f".format(km)} km • $min min"
                            dibujarRutaHaciaEmpresa(miPunto, puntos)
                        }
                    } catch (e: Exception) {
                        estadoRuta = "Error de red al calcular ruta."
                    }
                }
            }
    }

    val launcher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) calcularRutaDesdeMiGPS() else estadoRuta = "Permiso GPS denegado."
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(AzulMarino)
                .padding(12.dp)
        ) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Column {
                Text("Ubicación de la Empresa", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("TécnicoPro - Oficina y Taller", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val esriStreetMap = object : OnlineTileSourceBase(
                        "EsriStreetMap", 0, 19, 256, "",
                        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/")
                    ) {
                        override fun getTileURLString(pMapTileIndex: Long): String {
                            val zoom = MapTileIndex.getZoom(pMapTileIndex)
                            val x = MapTileIndex.getX(pMapTileIndex)
                            val y = MapTileIndex.getY(pMapTileIndex)
                            return "$baseUrl$zoom/$y/$x"
                        }
                    }

                    MapView(ctx).apply {
                        setTileSource(esriStreetMap)
                        setMultiTouchControls(true)
                        setUseDataConnection(true)
                        controller.setZoom(16.0)
                        controller.setCenter(destinoEmpresa)
                        overlays.add(Marker(this).apply {
                            position = destinoEmpresa
                            title = "Empresa TécnicoPro"
                            snippet = tecnico.sedeDireccion
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            showInfoWindow()
                        })
                        mapView = this
                    }
                },
                update = { it.onResume() }
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth().padding(14.dp).align(Alignment.TopCenter)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = AzulMarino, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(estadoRuta, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AzulMarino)
                }
            }

            Button(
                onClick = {
                    if (checkSelfPermission(context, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
                        calcularRutaDesdeMiGPS()
                    } else {
                        launcher.launch(ACCESS_FINE_LOCATION)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NaranjaFirma),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp).height(48.dp)
            ) {
                Icon(Icons.Default.Directions, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Trazar Ruta desde mi Ubicación", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

suspend fun obtenerRutaOSRM(inicio: GeoPoint, destino: GeoPoint): Triple<List<GeoPoint>, Double, Int>? =
    withContext(Dispatchers.IO) {
        val urlString = "https://router.project-osrm.org/route/v1/driving/" +
                "${inicio.longitude},${inicio.latitude};${destino.longitude},${destino.latitude}" +
                "?overview=full&geometries=geojson"
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "TecnicoProApp/1.0")
            connectTimeout = 10000
            readTimeout = 10000
        }
        val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        if (json.getString("code") == "Ok") {
            val route = json.getJSONArray("routes").getJSONObject(0)
            val distanciaKm = route.getDouble("distance") / 1000.0
            val duracionMin = (route.getDouble("duration") / 60.0).toInt()
            val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
            val puntos = (0 until coords.length()).map { i ->
                val par = coords.getJSONArray(i)
                GeoPoint(par.getDouble(1), par.getDouble(0))
            }
            Triple(puntos, distanciaKm, duracionMin)
        } else null
    }