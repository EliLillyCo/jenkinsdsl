package com.lilly.cirrus.jenkinsdsl.core

/**
 * Scope provides abstraction for a variable scope block in the DSL. This class provides following features:
 * <ul>
 * <li><b>Dynamic fields</b>: should be able to define custom/new fields within a DSL block in Jenkinsfile</li>
 * <li><b>Field inheritance (context chaining)</b>: to allow an inner block access fields defined in outer parallelBlocks
 * (from immediate parent to all the way up to the root block)</li>
 * <li><b>Field mutation security</b>: to prevent a block from directly modifying fields not defined in their own scope.
 * </li>
 * </ul>
 *
 * <b>Note</b>: A primitive type fields such as int, float, and boolean must not be statically defined in any subtype of
 * this class. Access to statically defined primitive field will break the current implementation.
 */
class Scope implements Serializable {
  private static final long serialVersionUID = -5437909802020439935L

  protected Scope parent = null
  protected Map<String, Object> properties = [:]


  /**
   * Expands the scope by setting a new parent. Variable defined in parent scope becoems visible after calling this
   * method.
   * @param parent The parent Scope.
   */
  void expandScope(Scope parent) {
    this.parent = parent
  }

  /**
   * Reduces the scope of the class by setting parent to null.
   */
  void reduceScope() {
    this.parent = null
  }

  /**
   * Returns the root scope.
   */
  Scope root() {
    if (!this.parent) return this
    return this.parent.root()
  }

  /**
   * Returns the farthest parent scope that has the given field defined.
   * @param field The name of the field.
   */
  Scope root(String field) {
    Scope root = null
    Scope iterator = this

    while (iterator) {
      if (iterator.hasLocalProperty(field)) {
        root = iterator
      }
      iterator = iterator.parent
    }

    return root
  }

  /**
   * Returns the nearest parent scope that has the given field defined.
   * @param field The name of the field.
   */
  Scope nearest(String field) {
    Scope iterator = this

    while (iterator) {
      if (iterator.hasLocalProperty(field)) {
        return iterator
      }
      iterator = iterator.parent
    }

    return null
  }

  /**
   * Returns all the parent scopes that has the given field defined.
   * @param field The name of the field.
   */
  List<Scope> all(String field) {
    def list = []
    Scope iterator = this

    while (iterator) {
      if (iterator.hasLocalProperty(field)) {
        list.add(iterator)
      }
      iterator = iterator.parent
    }

    return list
  }

  /**
   * Checks if the given field is defined in the current scope.
   * @param field The name of the field
   * @return true if the field exists and false otherwise
   */
  boolean hasLocalProperty(String name) {
    if (this.hasProperty(name))
      return true
    return this.@properties.containsKey(name)
  }

  /**
   * Get the value associated with the given field in the current scope.
   * @param field The name of the field
   * @return The value assocaited with the field
   */
  Object getLocalProperty(String name) {
    def value = null

    if (this.hasProperty(name))
      value = this.@"${name}"

    if (value != null)
      return value

    return this.@properties.get(name)
  }

  /**
   * Recursively looks up in the local and parent scope for a field.
   * @param name The name of the field
   * @return The value assocaited with the field
   */
  @Override
  Object getProperty(String name) {
    def value = this.getLocalProperty(name)

    if (value != null)
      return value

    if (this.@parent)
      return this.@parent.getProperty(name)

    return null
  }

  /**
   * Set the value of the given field in the local scope. <br/>
   *
   * As consequence of this implementation, all of the subclasses <b>must not</b> define a setter in traditional sense.
   * Instead of defining `setSomeField(String someField)`, define `someField(String someField)`.
   *
   * @param name Field name
   * @param value Field value
   */
  @Override
  void setProperty(String name, Object value) {
    if (this.hasProperty(name)) {
      this.@"${name}" = value
    }
    else {
      this.@properties[name] = value
    }
  }

  /**
   * This method returns the property defined in the closest Scope of the local object. It uses the `nearest()` method
   * to find the nearest scope that contains a property with the given name. If a scope object cannot be found, then a
   * NoSuchFieldException is thrown.
   * @param name property name to which to set the value
   * @param value object that will be set
   */
  void setNearestProperty(String name, Object value) {
    Object nearest = this.nearest(name)
    if (!nearest) {
      throw new NoSuchFieldException("No property found in the provided Scope hierarchy")
    }
    nearest.setProperty(name, value)
  }

  /**
   * This method gets called when we invoke a missing method in any of its subtypes. So, if method is missing, we
   * delegate the call to the outerscope (parent). This way a Stage class/subclass will be able to call methods defined in
   * a Pipeline class/subclass. This essentially enables runtime method inheritance through closure composition.
   *
   * @param name Method name
   * @param args Method args
   * @return The return value of the missing method after it is resolved
   * @throws MissingMethodException when method cannot be resolved to any parents
   */
  def methodMissing(String name, def args) {
    if (!this.@parent) {
      throw new MissingMethodException(name, this.getClass(), args)
    }

    return this.@parent.invokeMethod(name, args)
  }
}
