# Fórmula de Haversine

## Esta fórmula tiene los siguientes componentes:

Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2)

1. Diferencia de latitud: Math.pow(Math.sin(dLat / 2), 2)

• dLat / 2: Divide a la mitad la diferencia entre la latitud de origen y la de destino (ambas deben estar convertidas previamente a radianes).
• Math.sin(...): Calcula el seno trigonométrico de ese ángulo medio (esta función seno cuadrado del ángulo medio es precisamente lo que se conoce matemáticamente como haversine).
• Math.pow(..., 2): Eleva el resultado al cuadrado. Esto asegura que el valor sea siempre positivo (sin importar si vas hacia el norte o hacia el sur) y evita errores de precisión cuando las distancias son muy cortas (como unos pocos metros entre un pasajero y un conductor).

2. Corrección por la curvatura terrestre: Math.cos(lat1) * Math.cos(lat2)

• El problema: La distancia entre dos líneas de longitud (meridianos) no es constante; es máxima en el ecuador y se va estrechando hasta llegar a cero en los polos.
• La solución: Multiplicar los cosenos de ambas latitudes (lat1 y lat2, en radianes) actúa como un factor de escala que ajusta la distancia horizontal según qué tan cerca del ecuador o de los polos ocurra el viaje.

3. Diferencia de longitud ajustada: * Math.pow(Math.sin(dLon / 2), 2)

• Aplica exactamente el mismo cálculo de haversine que en el primer paso, pero para la diferencia de longitud (dLon), multiplicándolo por el factor de escala de los cosenos del paso anterior.

Esta línea es el segundo paso de la Fórmula de Haversine y se encarga de convertir el valor numérico intermedio a en un ángulo geométrico (medido en radianes) que separa ambos puntos desde el centro de la Tierra.
Cada componente de esta expresión realiza lo siguiente:

1. Math.sqrt(a) (Cateto opuesto)

• En el paso anterior, elevamos los senos al cuadrado (Math.pow(..., 2)), por lo que a representa una proporción al cuadrado (entre 0 y 1).
• Al sacar la raíz cuadrada (Math.sqrt(a)), recuperamos la proporción lineal equivalente al seno de la mitad del ángulo entre los dos puntos.

2. Math.sqrt(1 - a) (Cateto adyacente)

• Por la identidad trigonométrica fundamental (\(\sin^2(\theta) + \cos^2(\theta) = 1\)), si a es \(\sin^2(\theta)\), entonces 1 - a es exactamente igual a \(\cos^2(\theta)\).
• Al aplicarle Math.sqrt(1 - a), obtenemos el coseno de esa mitad de ángulo.

3. Math.atan2(y, x) (Arcotangente de dos argumentos)

• Esta función recibe el seno (y) y el coseno (x) y devuelve el ángulo exacto (en radianes) cuya tangente es y / x.
• ¿Por qué se usa atan2 en lugar de asin? Porque Math.atan2 es numéricamente estable en los extremos: evita divisiones por cero si los puntos están en lados opuestos de la Tierra (1 - a = 0) y mantiene alta precisión cuando la distancia entre el conductor y el pasajero es de apenas unos metros (a muy cercano a 0).

Paso final para obtener la distancia

El resultado de Math.atan2(...) representa la mitad del ángulo central entre las dos coordenadas; por eso siempre se multiplica por 2 y luego por el radio de la Tierra (6371.0 km):