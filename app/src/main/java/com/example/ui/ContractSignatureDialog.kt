package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

const val FULL_CONTRACT_TEXT = """FXESTUDIO CONTRATO DE SESIÓN FOTOGRÁFICA (Actualizado el 5 de septiembre del 2024)

Aclaraciones de Nuestro contrato de Sesión Fotográfica. Estimado Cliente, lea detalladamente nuestras condiciones del contrato de sesión fotográfico.

De nuestra parte (FXestudio):

Garantizamos la entrega del trabajo en soporte digital y con calidad antes del mes.
Estamos en el deber de responder todas las inquietudes hasta el término del contrato.
Nuestro estudio le informará y consultará en cada etapa del trabajo.

Reservación / Cancelación:

Usted puede reservar cualquier día disponible y sin tiempo de antelación.
La reservación será presencial, en el estudio, no por teléfono, ni por correo, excepto para fotos de los meses y algunos casos de clientes que se les imposible por lejanía.
La reservación y el contrato lo haremos solamente con personas mayores de edad y debe proporcionarnos su nombre completo, su número de teléfono, el nombre del o la modelo a fotografiar, debe escoger un día disponible, especificar el tema de fotografía, la locación, y su firma, aprobando que está de acuerdo con todo lo expuesto en este contrato.
Al reservar debe pagar un monto de dinero para asegurar su sesión fotográfica, este monto varia según la envergadura del trabajo y se incluye el precio total del día de la sesión.
Después de reservar usted nos debe llamar el día antes para confirmar que todo está en orden y se mantiene su sesión fotográfica. -En caso de cancelación usted lo deberá realizar con una semana de antelación y se le devolverá todo el dinero de la reservación. Si no cancela o no se presenta el día reservado nuestro estudio no le devolverá el dinero de la reservación, será nuestro pago por usted no cumplir con el contrato y ocupar un día que otro cliente pudo haber reservado. Seremos flexibles en situaciones de fuerza mayor y le propondremos aplazar la sesión para una nueva fecha.

Forma de Pago:

El pago es el día de la sesión de fotos. Si usted lo desea puede modificar la oferta antes convenida. En caso de ser menor el monto de dinero total del usado para pagar se le devolverá el resto.
Nuestros precios están representados en USD, pero aceptamos cualquier tipo de moneda en efectivo a como esté el cambio en el mercado (este valor es negociable y se usa el USD como referencia). Aceptamos el pago por transferencia en Zelle y hasta 3000 CUP por transferencia. - Respetaremos solo los precios en USD acordados en el momento de la reservación hasta el día de la sesión y no su tasa de cambio en otras monedas, si en este tiempo varía, usted deberá asumir los costos según el cambio el día de la sesión fotográfica.

En la sesión fotográfica:

El día de la sesión de fotos nuestro equipo de trabajadores le recibirá con una cálida bienvenida y se comenzará la preparación para la sesión. En todo momento nos dirigiremos principalmente a los tutores y trataremos de mantener un ambiente ameno y cordial para relajar al modelo ya que la fotografía puede usar un poco de tensión.
Recomendamos solo dos acompañantes con el modelo, los demás deben esperar fuera del estudio. En caso de foto familiar será la primera en realizarse.
Nuestro equipo siempre tendrá en cuenta los criterios del cliente durante la sesión fotográfica con respecto al maquillaje, poses, locaciones, estilos, etc.
En el caso de los bebés de meses nuestro equipo evitará tocarlos y si es necesario usaremos gel de manos. Todo el tiempo los tutores deben encargarse del modelo.

Selección de las fotos:

Al final de la sesión los clientes seleccionarán las fotos, nuestro equipo no intervendrá en el proceso de selección, excepto por algún criterio técnico. El asistente anotará los números de las fotografías con comentarios brindados por el cliente como: estilo, cambio de color, borrar algún objeto, agregar texto, recortar, etc.
Una vez seleccionadas las fotografías se diseña el álbum al gusto del cliente, las fotos del álbum deben ser de las mismas seleccionadas, si desea otra debe pagarla.
El cliente solamente tiene el derecho a las fotografías pagadas en formato original, editadas e impresas, las demás son propiedad del estudio. Nuestro estudio no brindará las fotografías restantes que no fueron seleccionadas, no pagadas.

Pago Final:

Después de la selección el cliente pagará el monto total de dinero de su oferta elegida, tendremos en cuenta el dinero de la reservación.
El Cliente (Tutor) debe autorizar con su firma si nos permite publicar sus fotos en las redes sociales.
El Cliente (mayor de edad) deberá aprobar con una firma que está de acuerdo con todo lo convenido en el contrato. El cliente no podrá hacer cambios en la selección de las fotos y hacerlo deberá pagar la nueva selección. La edición es un proceso artístico y técnico hermoso, complejo, que toma tiempo y cuesta mucho dinero.

Proceso de Edición:

Después que el cliente seleccione sus fotos, pague y firme el contrato, comenzará el proceso de edición de las fotos, este tiempo varía según el trabajo, desde 15 minutos a una semana. El cliente tiene la posibilidad de hacer cambios en la selección de las fotos en este tiempo y debe hacerlo en el estudio para actualizar el contrato. Una vez comenzado el proceso de edición de las fotos, el cliente no podrá cambiar la selección de las fotos y hacerlo deberá pagar la nueva selección.
Nuestros editores, solo editarán las fotos seleccionadas por el cliente y con el estilo y comentarios plasmados en el contrato. Una vez editadas las fotos, nuestros editores las enviarán para la aprobación del cliente por WhatsApp a través del número de teléfono en el contrato. Aclaramos que las fotos que llegan con mala calidad (con puntos, pixelada) pero es por WhatsApp, las fotos originales editadas tienen toda la calidad. Este es el momento en el que el cliente puede corregir algún aspecto de la edición (Estilo, color, texto, recorte, etc.) no cambiar las fotos, nuestros editores corregirán los aspectos brindados por el cliente y le enviará nuevamente las fotos editadas con el cambio.
Una vez aprobada la edición de las fotos por el cliente procederemos a la edición de los demás artículos (Álbum, Bolso, Taza, Llavero, etc.) y se los enviaremos para la aprobación también, después de corregidos y aprobados estos, nuestro estudio los enviará a los talleres para su confección. Una vez enviados no se podrán hacer cambios en el diseño, de hacerlo deberá pagar por el cambio. En el caso que nuestro estudio sea el que cometió algún error de diseño asumiremos los costos del cambio.

Tiempos de Entrega del Trabajo:

Los tiempos de entrega varían según la envergadura del trabajo y pueden demorar desde 15 minutos en el caso del servicio de entrega al momento, hasta un mes en el caso del primer año, bodas o 15 años. En caso de sobrepasar este tiempo, nuestro estudio contactará con el cliente y se llegará a un acuerdo con el mismo, si desea continuar, esperando más tiempo, y si no, se le devolverá el monto de dinero de la parte no entregada. Somos un estudio fotográfico, no fabricamos álbum, fotoBook o marcos, solo los encargamos a terceros.
No nos hacemos responsables de los tiempos que demoren la impresión de las Ampliaciones, fabricación del Álbum, fotoBook y Marcos. Los artículos no llegan de los talleres a nuestro estudio al mismo tiempo, nuestro equipo le informará del arribo de cada uno de ellos y el cliente decidirá recogerlos individualmente o esperar que esté todo el pedido.
Una vez aprobado y entregado todo el trabajo del contrato, en soporte digital y físico al cliente, nuestro estudio publicará si fue aprobada, las fotos en las redes sociales. Aunque el cliente apruebe la publicación de sus fotos en las redes, nuestro estudio no está obligado a publicarlas.

Relación entre ambas partes:

En todo el proceso del trabajo nuestro estudio está en la obligación de informar y responder las inquietudes del cliente, tratarle con respeto y cumplir con el contrato. En el caso de no cumplir alguna parte el estudio le devolverá solo el dinero de la parte no entregada.
No permitiremos la falta de respeto de parte del cliente hacia nuestro equipo de trabajo, en caso de ocurrir se cancelará el contrato.
Le recomendamos al cliente esperar con paciencia los tiempos convenidos en el contrato y evitar las llamadas reiterativas para no ocuparnos innecesariamente en nuestro trabajo.
Recomendamos cumplir con los horarios convenidos para no afectar el desarrollo de la sesión.
Aunque el contrato lo apruebe con firma, quedará legalmente anulado si no lo leyó todo lo expuesto.
Recomendamos seguirnos en las redes sociales y apoyarnos con (likes) o comentarios positivos.

Gracias por elegirnos y feliz sesión fotográfica."""

/** Lo que devuelve el diálogo cuando el cliente firma y confirma. */
data class ContratoFirmado(
    val firmaBytes: ByteArray?,
    val fotoClienteBytes: ByteArray?,
    val nombreCliente: String,
    val telefonoCliente: String
)

@Composable
fun ContractSignatureDialog(
    title: String = "Contrato de Sesión Fotográfica",
    // Texto editable desde Ajustes; en blanco se usa el de fábrica.
    contractText: String = "",
    // En el pedido del carrito todavía no se conocen los datos del cliente, así
    // que se piden aquí para poder enviarle también su copia por WhatsApp.
    pedirDatosCliente: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (ContratoFirmado) -> Unit
) {
    val textoContrato = contractText.ifBlank { FULL_CONTRACT_TEXT }
    var termsAccepted by remember { mutableStateOf(false) }
    var hasSignature by remember { mutableStateOf(false) }
    var exportSignatureFn by remember { mutableStateOf<(() -> ByteArray?)?>(null) }
    var fotoCliente by remember { mutableStateOf<ByteArray?>(null) }
    var nombreCliente by remember { mutableStateOf("") }
    var telefonoCliente by remember { mutableStateOf("") }
    val datosCompletos = !pedirDatosCliente ||
        (nombreCliente.isNotBlank() && telefonoCliente.filter { it.isDigit() }.length >= 8)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "FXestudio • Términos y firma obligatoria",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_contract_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Content
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Contract text box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = textoContrato,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    // Datos del cliente (solo en el pedido del carrito)
                    if (pedirDatosCliente) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Datos del cliente",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedTextField(
                                    value = nombreCliente,
                                    onValueChange = { nombreCliente = it },
                                    label = { Text("Nombre y apellidos") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("contract_client_name")
                                )
                                OutlinedTextField(
                                    value = telefonoCliente,
                                    onValueChange = { nuevo ->
                                        telefonoCliente = nuevo.filter { it.isDigit() || it == '+' }
                                    },
                                    label = { Text("Teléfono (WhatsApp)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("contract_client_phone")
                                )
                                Text(
                                    text = "Se usa para enviarle su copia del pedido por WhatsApp.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Acceptance Checkbox
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (termsAccepted)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = termsAccepted,
                                onCheckedChange = { termsAccepted = it },
                                modifier = Modifier.testTag("terms_checkbox")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "He leído y acepto los términos y condiciones del Contrato de Sesión Fotográfica de FXestudio",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Signature Box
                    SignaturePad(
                        modifier = Modifier.fillMaxWidth(),
                        onSignatureChanged = { hasSig, exportFn ->
                            hasSignature = hasSig
                            exportSignatureFn = exportFn
                        }
                    )

                    // Foto del cliente como respaldo de la confirmación.
                    ClientPhotoCapture(
                        fotoBytes = fotoCliente,
                        onFotoTomada = { fotoCliente = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Cancelar",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }

                    Button(
                        onClick = {
                            val signatureBytes = exportSignatureFn?.invoke()
                            onConfirm(
                                ContratoFirmado(
                                    firmaBytes = signatureBytes,
                                    fotoClienteBytes = fotoCliente,
                                    nombreCliente = nombreCliente.trim(),
                                    telefonoCliente = telefonoCliente.trim()
                                )
                            )
                        },
                        enabled = termsAccepted && datosCompletos,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(52.dp)
                            .testTag("sign_and_confirm_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Firmar y Confirmar",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
