private void addSort(
            final SearchRequestBuilder builder,
            final AnyTypeKind kind,
            final List<OrderByClause> orderBy) {

        AnyUtils attrUtils = anyUtilsFactory.getInstance(kind);

        for (OrderByClause clause : filterOrderBy(orderBy)) {
            String sortName = null;

            // Manage difference among external key attribute and internal JPA @Id
            String fieldName = "key".equals(clause.getField()) ? "id" : clause.getField();

            Field anyField = ReflectionUtils.findField(attrUtils.anyClass(), fieldName);
            if (anyField == null) {
                PlainSchema schema = schemaDAO.find(fieldName);
                if (schema != null) {
                    sortName = fieldName;
                }
            } else {
                sortName = fieldName;
            }

            if (sortName == null) {
                LOG.warn("Cannot build any valid clause from {}", clause);
            } else {
                builder.addSort(sortName, SortOrder.valueOf(clause.getDirection().name()));
            }
        }
    }