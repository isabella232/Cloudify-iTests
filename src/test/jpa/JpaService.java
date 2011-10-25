package test.jpa;

/**
 * JPA remoting service interface.
 * 
 * @author idan
 * @since 8.0.1
 *
 */
public interface JpaService {
	
	/**
	 * Gets the entity count in space of the provided type.
	 * @param type The type to return the count for.
	 * @return Entity count for the provided type. 
	 */
	Integer getEntityCount(Class<?> type);
	
	/**
	 * Gets the entity count in space of the provided type and entity Id.
	 * @param type The type to return the count for.
	 * @param id The entity id to count.
	 * @return Entity count for the provided type and Id.
	 */
	Integer getEntityCountById(Class<?> type, Object id);
	
}
