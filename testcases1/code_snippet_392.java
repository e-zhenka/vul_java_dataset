private void init() {
        // list up types that should be marshalled out like a value, without referential integrity tracking.
        addImmutableType(Result.class);

        // http://www.openwall.com/lists/oss-security/2017/04/03/4
        denyTypes(new Class[] { void.class, Void.class });

        registerConverter(new RobustCollectionConverter(getMapper(),getReflectionProvider()),10);
        registerConverter(new RobustMapConverter(getMapper()), 10);
        registerConverter(new ImmutableMapConverter(getMapper(),getReflectionProvider()),10);
        registerConverter(new ImmutableSortedSetConverter(getMapper(),getReflectionProvider()),10);
        registerConverter(new ImmutableSetConverter(getMapper(),getReflectionProvider()),10);
        registerConverter(new ImmutableListConverter(getMapper(),getReflectionProvider()),10);
        registerConverter(new CopyOnWriteMap.Tree.ConverterImpl(getMapper()),10); // needs to override MapConverter
        registerConverter(new DescribableList.ConverterImpl(getMapper()),10); // explicitly added to handle subtypes
        registerConverter(new Label.ConverterImpl(),10);

        // this should come after all the XStream's default simpler converters,
        // but before reflection-based one kicks in.
        registerConverter(new AssociatedConverterImpl(this), -10);

        registerConverter(new BlacklistedTypesConverter(), PRIORITY_VERY_HIGH); // SECURITY-247 defense

        registerConverter(new DynamicProxyConverter(getMapper()) { // SECURITY-105 defense
            @Override public boolean canConvert(Class type) {
                return /* this precedes NullConverter */ type != null && super.canConvert(type);
            }
            @Override public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                throw new ConversionException("<dynamic-proxy> not supported");
            }
        }, PRIORITY_VERY_HIGH);
    }