# Regras do otimizador (R8) para a versão release do Otimiza AI.

# Enums gravados nas preferências pelo NOME (VehicleType, NavigationApp, FuelType...):
# o R8 não pode renomear as constantes, senão as configurações salvas se perdem.
-keepclassmembers enum com.otimizaai.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Mensagens de erro mais legíveis em relatórios de falha.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
